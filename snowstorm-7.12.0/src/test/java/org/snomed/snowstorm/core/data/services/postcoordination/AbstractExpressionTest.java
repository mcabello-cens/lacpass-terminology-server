package org.snomed.snowstorm.core.data.services.postcoordination;

import com.google.common.collect.Lists;
import io.kaicode.elasticvc.api.VersionControlHelper;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.snomed.otf.snomedboot.testutil.ZipUtil;
import org.snomed.snowstorm.AbstractTest;
import org.snomed.snowstorm.core.data.domain.CodeSystem;
import org.snomed.snowstorm.core.data.domain.Concept;
import org.snomed.snowstorm.core.data.domain.Concepts;
import org.snomed.snowstorm.core.data.services.CodeSystemService;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.snomed.snowstorm.core.data.services.ServiceException;
import org.snomed.snowstorm.core.rf2.RF2Type;
import org.snomed.snowstorm.core.rf2.rf2import.ImportService;
import org.snomed.snowstorm.core.util.TimerUtil;
import org.snomed.snowstorm.mrcm.MRCMService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.snomed.snowstorm.core.data.domain.Concepts.ISA;

public abstract class AbstractExpressionTest extends AbstractTest {

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private ImportService importService;

    @Autowired
    private VersionControlHelper versionControlHelper;

    @Autowired
    private MRCMService mrcmService;

    @Autowired
    private CodeSystemService codeSystemService;

    protected ExpressionContext expressionContext;

    @BeforeEach
    public void setup() throws ServiceException, ReleaseImportException, IOException {
        conceptService.batchCreate(Lists.newArrayList(
                new Concept(Concepts.SNOMEDCT_ROOT),
                new Concept("421720008"),
                new Concept("405815000"),
                new Concept("49062001").addFSN("Device (physical object)"),
                new Concept("122456005").addFSN("Laser device").addRelationship(ISA, "49062001"),
                new Concept("7946007"),
                new Concept("405813007").addFSN("Procedure site - direct (attribute)"),
                new Concept("71388002").addFSN("Procedure").addRelationship(ISA, Concepts.SNOMEDCT_ROOT),
                new Concept("129264002").addFSN("Action (qualifier value)"),
                new Concept("129304002").addFSN("Excision - action").addRelationship(ISA, "129264002"),
                new Concept("260686004").addFSN("Method (attribute)"),
                new Concept("272741003").addFSN("Laterality (attribute)"),
                new Concept("182353008").addFSN("Side"),
                new Concept("24028007").addFSN("Right").addRelationship(ISA, "182353008"),
                new Concept("442083009").addFSN("Anatomical or acquired body structure (body structure)").addRelationship(ISA, Concepts.SNOMEDCT_ROOT),
                new Concept("15497006").addFSN("Ovarian structure").addRelationship(ISA, "442083009"),
                new Concept("388441000").addFSN("Horse"),

                // 83152002 |Oophorectomy| :
                //{ 260686004 |Method| = 129304002 |Excision - action|,
                //   405813007 |Procedure site - Direct| = 15497006 |Ovarian structure|,
                new Concept("83152002")
                        .addFSN("Oophorectomy")
                        .addRelationship(ISA, "71388002")
                        .addRelationship(1, "260686004 | method |", "129304002 |Excision - action|")
                        .addRelationship(1, "405813007 |Procedure site - Direct|", "15497006 |Ovarian structure|")


        ), "MAIN");

        File dummyMrcmImportFile = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/dummy-snomed-content/SnomedCT_MiniRF2_dummy_mrcm_snap");
        codeSystemService.createCodeSystem(new CodeSystem("SNOMEDCT", "MAIN"));
        String importJob = importService.createJob(RF2Type.SNAPSHOT, "MAIN", true, false);
        try (FileInputStream inputStream = new FileInputStream(dummyMrcmImportFile)) {
            importService.importArchive(importJob, inputStream);
        }
        expressionContext = new ExpressionContext("MAIN", versionControlHelper, mrcmService, new TimerUtil(""));
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        conceptService.deleteAll();
    }
}
