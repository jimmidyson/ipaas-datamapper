/**
 * Copyright (C) 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mediadriver.atlas.core.v2;

import com.mediadriver.atlas.api.v2.AtlasContext;
import com.mediadriver.atlas.api.v2.AtlasContextFactory;
import com.mediadriver.atlas.api.v2.AtlasException;
import com.mediadriver.atlas.api.v2.AtlasSession;
import com.mediadriver.atlas.mxbean.v2.AtlasContextMXBean;
import com.mediadriver.atlas.spi.v2.AtlasModule;
import com.mediadriver.atlas.spi.v2.AtlasModuleInfo;
import com.mediadriver.atlas.spi.v2.AtlasModuleMode;
import com.mediadriver.atlas.v2.AtlasMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class DefaultAtlasContext implements AtlasContext, AtlasContextMXBean {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAtlasContext.class);
	private ObjectName jmxObjectName;
	private final UUID uuid;
	private DefaultAtlasContextFactory factory;
	private AtlasMapping atlasMapping;
	private AtlasModule sourceModule;
	private AtlasModule targetModule;
	private Class<AtlasModule> sourceModuleClass;
	private Class<AtlasModule> targetModuleClass;
	private String sourceFormat;
	private String targetFormat;
	private Map<String, String> sourceProperties;
	private Map<String, String> targetProperties;
		
	public DefaultAtlasContext(AtlasMapping atlasMapping) throws AtlasException {
		this.factory = DefaultAtlasContextFactory.getInstance();
		this.uuid = UUID.randomUUID();
		this.atlasMapping = atlasMapping;
	}
	
	public DefaultAtlasContext(DefaultAtlasContextFactory factory, AtlasMapping atlasMapping) throws AtlasException {
		this.factory = factory;
		this.uuid = UUID.randomUUID();
		this.atlasMapping = atlasMapping;
	}
	
	/** 
	 * TODO: For dynamic re-load. This needs lock()
	 * 
	 * @throws AtlasException
	 */
	protected void init() throws AtlasException {
				
		registerJmx(this);

		List<AtlasModuleInfo> modules = factory.getModules();
		
		for (AtlasModuleInfo module : modules) {
			if(AtlasUtil.matchUriModule(module.getUri(), getSourceModuleUri())) {
				try {
					setSourceModuleClass((Class<AtlasModule>)Class.forName(module.getModuleClassName()));
					setSourceModule(getSourceModuleClass().newInstance());
					getSourceModule().setMode(AtlasModuleMode.SOURCE);
					getSourceModule().init();
				} catch (ClassNotFoundException e) {
					logger.error("Cannot find source ModuleClass " + module.toString(), e);
					throw new AtlasException("Cannot source ModuleClass: " + module.getModuleClassName(), e);
				} catch (ReflectiveOperationException e) {
					logger.error("Unable to initialize target module: " + module.toString(), e);
					throw new AtlasException("Unable to initialize target module: " + module.getModuleClassName(), e);
				}
			}
			if(AtlasUtil.matchUriModule(module.getUri(), getTargetModuleUri())) {
				try {
					setTargetModuleClass((Class<AtlasModule>)Class.forName(module.getModuleClassName()));
					setTargetModule(getSourceModuleClass().newInstance());
					getTargetModule().setMode(AtlasModuleMode.TARGET);
					getTargetModule().init();
				} catch (ClassNotFoundException e) {
					logger.error("Cannot find target ModuleClass: " + module.toString(), e);
					throw new AtlasException("Cannot find target ModuleClass: " + module.getModuleClassName(), e);
				} catch (ReflectiveOperationException e) {
					logger.error("Unable to initialize target module: " + module.toString(), e);
					throw new AtlasException("Unable to initialize target module: " + module.getModuleClassName(), e);
				}
			}
 		}		
	}
	
	protected void registerJmx(DefaultAtlasContext context) {
		try {
			setJmxObjectName(new ObjectName(getDefaultAtlasContextFactory().getJmxObjectName()+",context=Contexts,uuid="+uuid.toString()));
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, getJmxObjectName());
			if(logger.isDebugEnabled()) {
				logger.debug("Registered AtlasContext " + context.getUuid() + " with JMX");
			}
		} catch (Throwable t) {
			logger.warn("Failured to register AtlasContext " + context.getUuid() + " with JMX msg: " + t.getMessage(), t);
		}
	}
	
	/**
	 * process the session
	 * 
	 */
	@Override
	public void process(AtlasSession session) throws AtlasException {
		if(logger.isDebugEnabled()) {
			logger.debug("Begin process " + (session == null ? null : session.toString()));
		}
		
		
		// Replace w/ a preExecute / validation lifecycle phase
		/*
		if(session == null || session.getOutputStream() == null) {
			throw new AtlasException("Unable to marshal, invalid session object." + (session == null ? null : session.toString()));
		}
		
		
		if(getSourceModule() == null || getTargetModule() == null) {
			throw new AtlasException("Unable to marshal, invalid modules. InputModule: " + (getInputModule() == null ? null : getInputModule().toString()) + " OutputModule: " + (getOutputModule() == null ? null : getOutputModule().toString()));
		}
		*/
		
		getSourceModule().processInput(session);
		getTargetModule().processOutput(session);
		
		if(logger.isDebugEnabled()) {
			logger.debug("End process " + (session == null ? null : session.toString()));
		}
	}
	
	protected DefaultAtlasContextFactory getDefaultAtlasContextFactory() { return this.factory; }
		
	@Override
	public AtlasContextFactory getContextFactory() {
		return this.factory;
	}

	public AtlasMapping getAtlasMapping() {
		return atlasMapping;
	}

	public AtlasSession createSession() {
		return createSession(atlasMapping);
	}
	
	public AtlasSession createSession(AtlasMapping atlasMapping) {
		AtlasSession session = new DefaultAtlasSession();
		session.setAtlasContext(this);
		session.setAtlasMapping(atlasMapping);
		setDefaultSessionProperties(session);
		return session;
	}
	
	protected void setDefaultSessionProperties(AtlasSession session) {
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		df.setTimeZone(TimeZone.getDefault());
		session.getProperties().put("Atlas.CreatedDateTimeTZ", df.format(date));
	}
	
	public AtlasModule getSourceModule() {
		return sourceModule;
	}

	public void setSourceModule(AtlasModule sourceModule) {
		this.sourceModule = sourceModule;
	}

	public AtlasModule getTargetModule() {
		return targetModule;
	}

	public void setTargetModule(AtlasModule targetModule) {
		this.targetModule = targetModule;
	}

	public Class<AtlasModule> getSourceModuleClass() {
		return sourceModuleClass;
	}

	public void setSourceModuleClass(Class<AtlasModule> sourceModuleClass) {
		this.sourceModuleClass = sourceModuleClass;
	}

	public Class<AtlasModule> getTargetModuleClass() {
		return targetModuleClass;
	}

	public void setTargetModuleClass(Class<AtlasModule> targetModuleClass) {
		this.targetModuleClass = targetModuleClass;
	}

	public String getSourceFormat() {
		return sourceFormat;
	}

	public void setSourceFormat(String sourceFormat) {
		this.sourceFormat = sourceFormat;
	}

	public String getTargetFormat() {
		return targetFormat;
	}

	public void setTargetFormat(String targetFormat) {
		this.targetFormat = targetFormat;
	}

	public Map<String, String> getSourceProperties() {
		return sourceProperties;
	}

	public void setSourceProperties(Map<String, String> sourceProperties) {
		this.sourceProperties = sourceProperties;
	}

	public Map<String, String> getTargetProperties() {
		return targetProperties;
	}

	public void setTargetProperties(Map<String, String> targetProperties) {
		this.targetProperties = targetProperties;
	}

	protected void setJmxObjectName(ObjectName jmxObjectName) {
		this.jmxObjectName = jmxObjectName;
	}
	
	public ObjectName getJmxObjectName() {
		return this.jmxObjectName;
	}
	
	public String getSourceModuleUri() {
		if(getAtlasMapping() != null && getAtlasMapping().getSourceUri() != null) {
			return getAtlasMapping().getSourceUri();
		}
		return null;
	}
	
	public String getTargetModuleUri() {
		if(getAtlasMapping() != null && getAtlasMapping().getTargetUri() != null) {
			return getAtlasMapping().getTargetUri();
		}
		return null;
	}
	
	@Override
	public String getUuid() {
		return (this.uuid != null ? this.uuid.toString() : null);
	}
	
	@Override
	public String getVersion() {
		return this.getClass().getPackage().getImplementationVersion();
	}

	@Override
	public String getMappingName() {
		return (atlasMapping != null ? atlasMapping.getName() : null);
	}
	
	@Override
	public String getClassName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getThreadName() {
		return Thread.currentThread().getName();
	}

	@Override
	public String toString() {
		return "DefaultAtlasContext [jmxObjectName=" + jmxObjectName + ", uuid=" + uuid + ", factory=" + factory
				+ ", atlasMapping=" + atlasMapping + ", sourceModule=" + sourceModule + ", targetModule=" + targetModule
				+ ", sourceModuleClass=" + sourceModuleClass + ", targetModuleClass=" + targetModuleClass
				+ ", sourceFormat=" + sourceFormat + ", targetFormat=" + targetFormat + ", sourceProperties="
				+ sourceProperties + ", targetProperties=" + targetProperties + "]";
	}	
}
