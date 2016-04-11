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
import static org.forgerock.json.JsonValue.json

import org.forgerock.http.header.ContentTypeHeader
import org.forgerock.http.protocol.Request
import org.forgerock.http.protocol.Response
import org.forgerock.http.protocol.Status
import org.forgerock.http.routing.UriRouterContext
import org.forgerock.openig.heap.HeapImpl
import org.forgerock.openig.heap.Heaplets
import org.forgerock.openig.heap.Keys
import org.forgerock.openig.heap.Name
import org.forgerock.openig.io.TemporaryStorage
import org.forgerock.openig.log.NullLogSink
import org.forgerock.services.context.Context
import org.forgerock.services.context.RootContext

import spock.lang.Specification
import spock.lang.Unroll

class ResourcesHandlerSpec extends Specification {

    Context context
    Request request

    void setup() {
        context = new UriRouterContext(new RootContext(), "/openig", "/index.html", new HashMap<>())
        request = new Request().setUri("http://localhost/openig/index.html").setMethod("GET")
    }

    def "Should instantiate ResourcesHandler from JSON configuration"() {
        given:
        Name name = Name.of("this")
        HeapImpl heap = new HeapImpl(Name.of("heap"))
        heap.put(Keys.LOGSINK_HEAP_KEY, new NullLogSink())
        heap.put(Keys.TEMPORARY_STORAGE_HEAP_KEY, new TemporaryStorage())
        def heaplet = Heaplets.getHeaplet(ResourcesHandler)

        when:
        def config = [ directories: [ '${system[\'user.home\']}/public' ] ]
        def handler = heaplet.create(name, json(config), heap)

        then:
        handler.resourceSets.size() == 1
    }

    @Unroll
    def "Should only accept GET requests"() {
        given:
        ResourcesHandler handler = new ResourcesHandler(
                resourceSets: [ { new MockedResource("index.html", 1) } as ResourceSet ])

        when:
        Response response = handler.handle(context, new Request().setMethod(method)).get()

        then:
        response.status == status

        where:
        method   || status
        "GET"    || Status.OK
        "POST"   || Status.METHOD_NOT_ALLOWED
        "HEAD"   || Status.METHOD_NOT_ALLOWED
        "PATCH"  || Status.METHOD_NOT_ALLOWED
        "DELETE" || Status.METHOD_NOT_ALLOWED
    }

    def "Should serve resource"() {
        given:
        ResourcesHandler handler = new ResourcesHandler(
                resourceSets: [ { new MockedResource("index.html", 4200) } as ResourceSet ])

        when:
        Response response = handler.handle(context, new Request().setMethod("GET")).get()

        then:
        response.status == Status.OK
        response.headers.get(ContentTypeHeader).type == "text/html"
        response.headers.getFirst('Last-Modified') == "Thu, 01 Jan 1970 00:00:04 GMT"
        response.entity.string == "Hello World"
    }

    def "Should serve 'Not Modified' resource"() {
        given:
        ResourcesHandler handler = new ResourcesHandler(
                resourceSets: [ { new MockedResource("index.html", 4200) } as ResourceSet ])
        def request = new Request().setMethod("GET")
        request.headers.put('If-Modified-Since', formatDate(new Date(5300)))

        when:
        Response response = handler.handle(context, request).get()

        then:
        response.status == ResourcesHandler.NOT_MODIFIED
        response.headers.getFirst('Last-Modified') == "Thu, 01 Jan 1970 00:00:04 GMT"
        response.entity.isRawContentEmpty()
    }

    def "Should serve modified resource"() {
        given:
        ResourcesHandler handler = new ResourcesHandler(
                resourceSets: [ { new MockedResource("index.html", 4200) } as ResourceSet ])
        def request = new Request().setMethod("GET")
        request.headers.put('If-Modified-Since', formatDate(new Date(1024)))

        when:
        Response response = handler.handle(context, request).get()

        then:
        response.status == Status.OK
        response.headers.get(ContentTypeHeader).type == "text/html"
        response.headers.getFirst('Last-Modified') == "Thu, 01 Jan 1970 00:00:04 GMT"
        response.entity.string == "Hello World"
    }

    def "Should return 404 when resource is not found"() {
        given:
        ResourcesHandler handler = new ResourcesHandler(resourceSets: [ { null } as ResourceSet ])
        def request = new Request().setMethod("GET")

        when:
        Response response = handler.handle(context, request).get()

        then:
        response.status == Status.NOT_FOUND
    }

    static class MockedResource extends AbsResource {
        private final String path
        private final int lastModified

        MockedResource(String path, Integer lastModified) {
            this.path = path
            this.lastModified = lastModified
        }

        @Override
        InputStream open() throws IOException {
            return new ByteArrayInputStream("Hello World".getBytes())
        }

        @Override
        String getPath() {
            return path
        }

        @Override
        long getLastModified() {
            return lastModified
        }
    }
}
