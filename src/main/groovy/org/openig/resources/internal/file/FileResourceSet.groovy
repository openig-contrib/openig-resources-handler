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

package org.openig.resources.internal.file

import org.openig.resources.internal.Resource
import org.openig.resources.internal.ResourceSet

/**
 * Created by guillaume on 10/04/16.
 */
class FileResourceSet implements ResourceSet {

    final File root

    FileResourceSet(final File root) {
        this.root = root
    }

    @Override
    Resource find(final String path) {
        def canonical = new File(root, path).canonicalFile
        // Make sure we don't serve resources outside of the root directory
        if (canonical.isFile() && canonical.path.startsWith(root.path)) {
            return new FileResource(file: canonical, path: path)
        }
        return null;
    }
}
