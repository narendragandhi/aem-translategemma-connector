package com.example.aem.translation.impl;

import com.example.aem.translation.service.MSMDeltaService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

@Component(service = MSMDeltaService.class)
public class MSMDeltaServiceImpl implements MSMDeltaService {

    private static final Logger LOG = LoggerFactory.getLogger(MSMDeltaServiceImpl.class);
    private static final String PN_LAST_ROLLED_OUT = "cq:lastRolledout";
    private static final String PN_LAST_TRANSLATION_UPDATE = "cq:lastTranslationUpdate_"; // Prefix with lang

    @Override
    public boolean isTranslationRequired(Resource resource, String targetLanguage) {
        if (resource == null) return true;

        ValueMap vm = resource.getValueMap();
        Calendar lastRolledOut = vm.get(PN_LAST_ROLLED_OUT, Calendar.class);
        Calendar lastTranslation = vm.get(PN_LAST_TRANSLATION_UPDATE + targetLanguage, Calendar.class);

        // If it's never been rolled out (e.g., local edit), it needs translation
        if (lastRolledOut == null) return true;

        // If it's never been translated to this language, it needs translation
        if (lastTranslation == null) return true;

        // If rolled out AFTER last translation, it means master content changed
        boolean required = lastRolledOut.after(lastTranslation);
        
        LOG.debug("Delta Check for {}: Required={}, RolledOut={}, Translated={}", 
            resource.getPath(), required, lastRolledOut.getTime(), lastTranslation.getTime());
            
        return required;
    }

    @Override
    public void markAsTranslated(Resource resource, String targetLanguage) {
        if (resource == null) return;

        ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
        if (mvm != null) {
            mvm.put(PN_LAST_TRANSLATION_UPDATE + targetLanguage, Calendar.getInstance());
            try {
                resource.getResourceResolver().commit();
            } catch (PersistenceException e) {
                LOG.error("Failed to mark resource as translated: {}", resource.getPath(), e);
            }
        }
    }
}
