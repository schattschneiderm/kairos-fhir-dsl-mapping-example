package projects.cxx.napkon.num_hgw

import de.kairos.fhir.centraxx.metamodel.Consent
import de.kairos.fhir.centraxx.metamodel.ConsentType
import de.kairos.fhir.centraxx.metamodel.IdContainerType
import de.kairos.fhir.centraxx.metamodel.PatientStudy
import org.hl7.fhir.r4.model.ResearchSubject

import static de.kairos.fhir.centraxx.metamodel.AbstractEntity.ID
import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.ID_CONTAINER_TYPE
import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.PSN
import static de.kairos.fhir.centraxx.metamodel.RootEntities.patient
import static de.kairos.fhir.centraxx.metamodel.RootEntities.patientMasterDataAnonymous
import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyMember

/**
 * Represented by CXX StudyMember
 * @author Mario Schattschneider
 */
researchSubject {

  id = "ResearchSubject/" + context.source[patient().patientContainer().patientStudies().id()]

  status = ResearchSubject.ResearchSubjectStatus.ONSTUDY

  identifier {
    type {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/v2-0203"
        code = "NUM_SUEP"
      }
    }
    system = "urn:centraxx"
    value = context.source[studyMember().studyMemberId()]
  }

  def studyOid = context.source[studyMember().study().id()]
  study {
    reference = "ResearchStudy/" + studyOid
  }

  final def idContainer = context.source[patientMasterDataAnonymous().patientContainer().idContainer()]?.find {
    "LIMSPSN" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
  }
  //if (idContainer) {
    individual {
      identifier {
        value = 12354//idContainer[PSN]
        type {
          coding {
            system = "urn:centraxx"
            code = "MPI"
          }
        }
      }
    }
     // "Patient/" + context.source[studyMember().patientContainer().id()]
  //}

  def studyConsent = context.source[studyMember().patientContainer().consents()].find { consent ->
    return studyOid == consent[Consent.CONSENT_TYPE][ConsentType.FLEXI_STUDY]?.getAt(ID)
  }
  if (studyConsent) {
    consent {
      reference = "Consent/" + studyConsent[ID]
    }
  }

  assignedArm = context.source[studyMember().enrollment().patientGroupName()]
  actualArm = context.source[studyMember().enrollment().patientGroupName()]

  def patientStudy = context.source[studyMember().patientContainer().patientStudies()].find { patientStudy ->
    return studyOid == patientStudy[PatientStudy.FLEXI_STUDY]?.getAt(ID)
  }
  if (patientStudy) {
    def memberFrom = patientStudy[PatientStudy.MEMBER_FROM]
    def memberUntil = patientStudy[PatientStudy.MEMBER_UNTIL]
    period {
      if (memberFrom) {
        start {
          date = memberFrom
        }
      }
      if (memberUntil) {
        end {
          date = memberUntil
        }
      }
    }
  }
}
