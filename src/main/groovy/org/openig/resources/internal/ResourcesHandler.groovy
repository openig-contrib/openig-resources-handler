/*
 * Copyright 2016 ForgeRock AS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openig.resources.internal

import static org.forgerock.http.header.HeaderUtil.formatDate
import static org.forgerock.http.header.HeaderUtil.parseDate
import static org.forgerock.http.io.IO.newBranchingInputStream
import static org.forgerock.http.protocol.Response.newResponsePromise
import static org.forgerock.http.protocol.Status.METHOD_NOT_ALLOWED
import static org.forgerock.http.protocol.Status.NOT_FOUND
import static org.forgerock.http.protocol.Status.OK
import static org.forgerock.openig.util.JsonValues.asString

import org.forgerock.http.Handler
import org.forgerock.http.header.ContentTypeHeader
import org.forgerock.http.protocol.Request
import org.forgerock.http.protocol.Response
import org.forgerock.http.protocol.Status
import org.forgerock.http.routing.UriRouterContext
import org.forgerock.openig.heap.GenericHeapObject
import org.forgerock.services.context.Context
import org.forgerock.util.promise.NeverThrowsException
import org.forgerock.util.promise.Promise
import org.openig.groovy.heaplet.Attribute
import org.openig.groovy.heaplet.Heaplet
import org.openig.groovy.heaplet.Transform
import org.openig.resources.internal.file.FileResourceSet
import org.openig.resources.media.MediaTypes

/**
 * Created by guillaume on 09/04/16.
 */
@Heaplet
class ResourcesHandler extends GenericHeapObject implements Handler {

    static final Status NOT_MODIFIED = Status.valueOf(304, "Not Modified")

    @Attribute("directories")
    @Transform({ new FileResourceSet(new File(asString(it)).absoluteFile) })
    List<ResourceSet> resourceSets;

    @Override
    Promise<Response, NeverThrowsException> handle(final Context context, final Request request) {

        // Reject any non-GET methods
        if ("GET" != request.method) {
            return newResponsePromise(new Response(METHOD_NOT_ALLOWED))
        }

        // Get the resource path to look for
        UriRouterContext urc = context.asContext(UriRouterContext);
        def target = urc.remainingUri

        // Test path in every root and return the first match
        for (ResourceSet resourceSet : resourceSets) {

            def resource = resourceSet.find(target)
            if (resource) {

                def response = new Response()
                // last modified
                response.headers[ 'Last-Modified' ] = formatDate(new Date(resource.lastModified))

                // cached ?
                def since = request.headers.getFirst('If-Modified-Since')
                if (since) {
                    if (!resource.hasChangedSince(parseDate(since).time)) {
                        response.status = NOT_MODIFIED
                        return newResponsePromise(response)
                    }
                }

                // not cached
                response.status = OK
                response.entity.rawContentInputStream = newBranchingInputStream(resource.open(), storage)

                // content-type
                def ext = resource.path.substring(resource.path.lastIndexOf('.') + 1)
                def mediaType = MediaTypes.instance.getType(ext)
                if (mediaType) {
                    response.headers[ ContentTypeHeader.NAME ] = mediaType
                }

                return newResponsePromise(response)
            }
        }
        return newResponsePromise(new Response(NOT_FOUND))
    }
}
