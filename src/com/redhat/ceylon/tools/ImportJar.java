/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.redhat.ceylon.tools;

import java.io.File;

import com.redhat.ceylon.cmr.api.ArtifactContext;
import com.redhat.ceylon.cmr.api.Logger;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.impl.CMRException;


public class ImportJar {

    private String module;
    private String version;
    private String out;
    private String user;
    private String pass;
    private String jarFile;
    private boolean verbose;
    private Logger log = new CMRLogger();

    ImportJar(String moduleSpec, String out, String user, String pass, String jarFile, boolean verbose){
        parseModuleSpec(moduleSpec);
        this.out = out;
        this.user = user;
        this.pass = pass;
        this.jarFile = jarFile;
        this.verbose = verbose;
        checkJarFile();
    }

    private void checkJarFile() {
        if(jarFile == null || jarFile.isEmpty())
            throw new ImportJarException("error.jarFile.empty");
        File f = new File(jarFile);
        if(!f.exists())
            throw new ImportJarException("error.jarFile.doesNotExist");
        if(f.isDirectory())
            throw new ImportJarException("error.jarFile.isDirectory");
        if(!f.canRead())
            throw new ImportJarException("error.jarFile.notReadable");
        if(!f.getName().toLowerCase().endsWith(".jar"))
            throw new ImportJarException("error.jarFile.notJar");
    }

    private void parseModuleSpec(String moduleSpec) {
        if(moduleSpec == null || moduleSpec.isEmpty())
            throw new ImportJarException("error.moduleSpec.empty");
        int sep = moduleSpec.indexOf("/");
        if(sep != -1){
            this.module = moduleSpec.substring(0, sep);
            if(this.module.isEmpty())
                throw new ImportJarException("error.moduleSpec.noName");
            this.version = moduleSpec.substring(sep+1);
            if(this.version.isEmpty())
                throw new ImportJarException("error.moduleSpec.noVersion");
        }else{
            if(!"default".equals(moduleSpec))
                throw new ImportJarException("error.moduleSpec.missingVersion");
            this.module = moduleSpec;
            this.version = null;
        }
    }
    
    public void publish(){
        RepositoryManager outputRepository = com.redhat.ceylon.compiler.java.util.Util.makeOutputRepositoryManager(this.out, log, user, pass);

        ArtifactContext context = new ArtifactContext(module, version, ArtifactContext.JAR);
        context.setForceOperation(true);
        try{
            outputRepository.putArtifact(context, new File(jarFile));
            String sha1 = ShaSigner.sha1(jarFile, log);
            if(sha1 != null){
                File shaFile = ShaSigner.writeSha1(sha1, log);
                if(shaFile != null){
                    try{
                        ArtifactContext sha1Context = context.getSha1Context();
                        outputRepository.putArtifact(sha1Context, shaFile);
                    }finally{
                        shaFile.delete();
                    }
                }
            }
        }catch(CMRException x){
            throw new ImportJarException("error.failedWriteArtifact", new Object[]{context, x.getLocalizedMessage()}, x);
        }catch(Exception x){
            // FIXME: remove when the whole CMR is using CMRException
            throw new ImportJarException("error.failedWriteArtifact", new Object[]{context, x.getLocalizedMessage()}, x);
        }
    }

    public class CMRLogger implements Logger {

        @Override
        public void error(String str) {
            throw new ImportJarException("error.cmrError", new Object[]{str}, null);
        }

        @Override
        public void warning(String str) {
            System.err.println(ImportJarMessages.msg("log.warning", str));
        }

        @Override
        public void info(String str) {
            System.err.println(ImportJarMessages.msg("log.info", str));
        }

        @Override
        public void debug(String str) {
            if(verbose)
                System.err.println(ImportJarMessages.msg("log.debug", str));
        }
    }
}