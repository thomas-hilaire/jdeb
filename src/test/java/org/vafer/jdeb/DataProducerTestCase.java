/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vafer.jdeb;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.ar.NonClosingInputStream;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;

public final class DataProducerTestCase extends TestCase {

    public void testCreation() throws Exception {

        final Processor processor = new Processor(new Console() {
            public void info( String s ) {
            }

            public void warn( String s ) {
            }
        }, null);

        final File control = new File(getClass().getResource("deb/control/control").toURI());
        final File archive1 = new File(getClass().getResource("deb/data.tgz").toURI());
        final File archive2 = new File(getClass().getResource("deb/data.tar.bz2").toURI());
        final File archive3 = new File(getClass().getResource("deb/data.zip").toURI());
        final File directory = new File(getClass().getResource("deb/data").toURI());

        final DataProducer[] data = new DataProducer[] {
            new DataProducerArchive(archive1, null, null, null),
            new DataProducerArchive(archive2, null, null, null),
            new DataProducerArchive(archive3, null, null, null),
            new DataProducerDirectory(directory, null, new String[] { "**/.svn/**" }, null)
        };

        final File deb = File.createTempFile("jdeb", ".deb");

        final PackageDescriptor packageDescriptor = processor.createDeb(new File[] { control }, data, deb, "gzip");

        assertTrue(packageDescriptor.isValid());

        final Set filesInDeb = new HashSet();

        final ArArchiveInputStream ar = new ArArchiveInputStream(new FileInputStream(deb));
        while (true) {
            final ArArchiveEntry arEntry = ar.getNextArEntry();
            if (arEntry == null) {
                break;
            }

            if ("data.tar.gz".equals(arEntry.getName())) {

                final TarInputStream tar = new TarInputStream(new GZIPInputStream(new NonClosingInputStream(ar)));

                while (true) {
                    final TarEntry tarEntry = tar.getNextEntry();
                    if (tarEntry == null) {
                        break;
                    }

                    filesInDeb.add(tarEntry.getName());
                }

                tar.close();
                break;
            }
            for (int i = 0; i < arEntry.getLength(); i++) {
                ar.read();
            }
        }

        ar.close();

        assertTrue("testfile wasn't found in the package", filesInDeb.contains("./test/testfile"));
        assertTrue("testfile2 wasn't found in the package", filesInDeb.contains("./test/testfile2"));
        assertTrue("testfile3 wasn't found in the package", filesInDeb.contains("./test/testfile3"));
        assertTrue("testfile4 wasn't found in the package", filesInDeb.contains("./test/testfile4"));

        assertTrue("Cannot delete the file " + deb, deb.delete());
    }
}
