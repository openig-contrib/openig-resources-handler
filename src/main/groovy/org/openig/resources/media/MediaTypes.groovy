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

package org.openig.resources.media

/**
 * Created by guillaume on 10/04/16.
 */
@Singleton(strict = false)
class MediaTypes {

    def extensions = [ : ]

    private MediaTypes() {
        getClass().getResourceAsStream("media-types.properties")
                  .withStream { stream ->
            Properties p = new Properties()
            p.load(stream)
            for (String type : p.stringPropertyNames()) {
                String ext = p.getProperty(type, "")
                ext.split().each {
                    extensions.put(it, type)
                }
            }
        }
    }

    String getType(String ext) {
        extensions[ ext ]
    }
}
