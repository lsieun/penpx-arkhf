package org.sonatype.licensing.trial.internal;

import codeguard.licensing.tpg;
import codeguard.licensing.zsv;
import codeguard.licensing.zts;
import com.sonatype.nexus.licensing.ext.NexusProfessionalFeature;
import de.schlichtherle.license.LicenseContent;

import java.io.File;
import java.util.Date;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.CustomLicenseContent;
import org.sonatype.licensing.LicenseContentException;
import org.sonatype.licensing.LicenseKey;
import org.sonatype.licensing.LicenseKeyRequest;
import org.sonatype.licensing.LicenseValidator;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.FeatureSet;
import org.sonatype.licensing.feature.FeatureValidator;
import org.sonatype.licensing.trial.TrialLicenseManager;
import org.sonatype.licensing.trial.TrialLicenseParam;

@Named("licensing.default")
public class DefaultTrialLicenseManager implements TrialLicenseManager {
    private final Logger evv = LoggerFactory.getLogger(getClass());

    private final Provider<LicenseKey> cgs;

    private final FeatureValidator fmh;

    private final LicenseValidator wst;

    public DefaultTrialLicenseManager(Provider<LicenseKey> paramProvider, FeatureValidator paramFeatureValidator) {
        this(paramProvider, paramFeatureValidator, (LicenseValidator) new zsv());
    }

    @Inject
    public DefaultTrialLicenseManager(Provider<LicenseKey> paramProvider, FeatureValidator paramFeatureValidator, @Nullable LicenseValidator paramLicenseValidator) {
        this.cgs = paramProvider;
        this.fmh = paramFeatureValidator;
        this.wst = paramLicenseValidator;
    }

    public LicenseKey createLicense(TrialLicenseParam paramTrialLicenseParam, LicenseKeyRequest paramLicenseKeyRequest) throws LicensingException {
        LicenseKey licenseKey = (LicenseKey) this.cgs.get();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(zts.class.getClassLoader());
            (new tpg(paramTrialLicenseParam, this.wst)).itm((LicenseContent) paramLicenseKeyRequest.getLicenseContent(), paramLicenseKeyRequest.getLicenseKeyFile());
            licenseKey.populateFromLicenseContent(paramLicenseKeyRequest.getLicenseContent());
        } catch (LicenseContentException licenseContentException) {
            if (this.evv.isTraceEnabled()) {
                this.evv.trace("createLicense", (Throwable) licenseContentException);
            }
            throw itm(licenseKey, licenseContentException);
        } catch (Exception exception) {
            if (this.evv.isTraceEnabled()) {
                this.evv.trace("createLicense", exception);
            }
            throw new LicensingException("Unable to create license: " + itm(exception), exception);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return licenseKey;
    }

    public LicenseKey installLicense(TrialLicenseParam paramTrialLicenseParam, File paramFile) throws LicensingException {
        LicenseKey licenseKey = (LicenseKey) this.cgs.get();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(zts.class.getClassLoader());
            CustomLicenseContent customLicenseContent = (CustomLicenseContent) (new tpg(paramTrialLicenseParam, this.wst)).itm(paramFile);
            licenseKey.populateFromLicenseContent(customLicenseContent);
        } catch (LicenseContentException licenseContentException) {
            if (this.evv.isTraceEnabled())
                this.evv.trace("installLicense", (Throwable) licenseContentException);
            throw itm(licenseKey, licenseContentException);
        } catch (Exception exception) {
            if (this.evv.isTraceEnabled())
                this.evv.trace("installLicense", exception);
            throw new LicensingException("Unable to install license: " + itm(exception), exception);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return licenseKey;
    }

    public void uninstallLicense(TrialLicenseParam paramTrialLicenseParam) throws LicensingException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(zts.class.getClassLoader());
            (new tpg(paramTrialLicenseParam, this.wst)).bao();
        } catch (LicenseContentException licenseContentException) {
            if (this.evv.isTraceEnabled())
                this.evv.trace("uninstallLicense", (Throwable) licenseContentException);
            throw new LicensingException(itm(licenseContentException), licenseContentException);
        } catch (Exception exception) {
            if (this.evv.isTraceEnabled())
                this.evv.trace("uninstallLicense", exception);
            throw new LicensingException("Unable to uninstall license: " + itm(exception), exception);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    public void verifyLicense(LicenseKey key){

        boolean bool = this.evv.isTraceEnabled();
        if (bool) {
            this.evv.trace("verifyLicense (1) param={}", key);
        }
        this.evv.info("\n*************************************************************\n***** Nexus Repository Manager 3.23 Licensing By Zayki  *****\n*************************************************************");
        key.setContactName("aenolife");
        key.setContactCompany("www.aeonlife.com.cn");
        key.setContactEmailAddress("zyjsuper@aeonlife.com.cn");
        key.setContactTelephone("+860000000000");
        key.setContactCountry("China");
        key.setEvaluation(false);
        key.setExpirationDate(new Date(2035 - 1900, 1, 1));
        key.setEffectiveDate(new Date(2025 - 1900, 1, 1));
        FeatureSet fs = new FeatureSet();
        fs.addFeature(new NexusProfessionalFeature());
        key.setFeatureSet(fs);
    }

    public LicenseKey verifyLicense(TrialLicenseParam paramTrialLicenseParam) throws LicensingException {
        boolean bool = this.evv.isTraceEnabled();
        if (bool) {
            this.evv.trace("verifyLicense (1) param={}", paramTrialLicenseParam);
        }

        LicenseKey licenseKey = (LicenseKey) this.cgs.get();
        if (bool) {
            this.evv.trace("verifyLicense (2) key={}", licenseKey);
        }

        verifyLicense(licenseKey);
        return licenseKey;

//        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//        try {
//            Thread.currentThread().setContextClassLoader(zts.class.getClassLoader());
//            CustomLicenseContent customLicenseContent = (CustomLicenseContent) (new tpg(paramTrialLicenseParam, this.wst)).mif();
//            if (bool) {
//                this.evv.trace("verifyLicense (3) content={}", customLicenseContent);
//            }
//            licenseKey.populateFromLicenseContent(customLicenseContent);
//        } catch (LicenseContentException licenseContentException) {
//            if (bool)
//                this.evv.trace("verifyLicense", (Throwable) licenseContentException);
//            throw itm(licenseKey, licenseContentException);
//        } catch (Exception exception) {
//            if (bool)
//                this.evv.trace("verifyLicense", exception);
//            throw new LicensingException("Unable to verify license: " + itm(exception), exception);
//        } finally {
//            Thread.currentThread().setContextClassLoader(classLoader);
//        }
//        if (bool)
//            this.evv.trace("verifyLicense (4) key={}", licenseKey);
//        return licenseKey;
    }

    public LicenseKey verifyLicense(TrialLicenseParam paramTrialLicenseParam, File paramFile) throws LicensingException {
        boolean bool = this.evv.isTraceEnabled();
        if (bool)
            this.evv.trace("verifyLicense (1) param={},file={}", paramTrialLicenseParam, paramFile);
        LicenseKey licenseKey = (LicenseKey) this.cgs.get();
        if (bool)
            this.evv.trace("verifyLicense (2) key={}", licenseKey);


        verifyLicense(licenseKey);
        return licenseKey;
//        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//        try {
//            Thread.currentThread().setContextClassLoader(zts.class.getClassLoader());
//            CustomLicenseContent customLicenseContent = (new tpg(paramTrialLicenseParam, this.wst)).rkn(paramFile);
//            licenseKey.populateFromLicenseContent(customLicenseContent);
//        } catch (LicenseContentException licenseContentException) {
//            if (bool)
//                this.evv.trace("verifyLicense", (Throwable) licenseContentException);
//            throw itm(licenseKey, licenseContentException);
//        } catch (Exception exception) {
//            if (bool)
//                this.evv.trace("verifyLicense", exception);
//            throw new LicensingException("Unable to verify license", exception);
//        } finally {
//            Thread.currentThread().setContextClassLoader(classLoader);
//        }
//        return licenseKey;
    }

    public void validateFeature(LicenseKey paramLicenseKey, Feature paramFeature) throws LicensingException {
        this.fmh.validate(paramFeature, paramLicenseKey);
    }

    private String itm(Exception paramException) {
        String str = (paramException.getLocalizedMessage() != null) ? paramException.getLocalizedMessage() : paramException.getMessage();
        return (str != null) ? str : paramException.getClass().getSimpleName();
    }

    private LicensingException itm(LicenseKey paramLicenseKey, LicenseContentException paramLicenseContentException) {
        paramLicenseKey.populateFromLicenseContent((CustomLicenseContent) paramLicenseContentException.getLicenseContent());
        return new LicensingException(paramLicenseKey, itm((Exception) paramLicenseContentException), paramLicenseContentException);
    }
}
