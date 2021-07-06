package projects.cxx.v2

import de.kairos.centraxx.common.types.GenderType
import de.kairos.fhir.centraxx.metamodel.IdContainerType

import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.ID_CONTAINER_TYPE
import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.PSN
import static de.kairos.fhir.centraxx.metamodel.PatientMaster.GENDER_TYPE
import static de.kairos.fhir.centraxx.metamodel.RootEntities.patientMasterDataAnonymous

/**
 * Represented by a CXX PatientMasterDataAnonymous
 * @author Mike Wähnert
 * @since v.1.5.0, CXX.v.3.17.1.5
 */
patient {

  id = "Patient/" + context.source[patientMasterDataAnonymous().patientContainer().id()]

  final def idContainer = context.source[patientMasterDataAnonymous().patientContainer().idContainer()]?.find {
    "LIMSPSN" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
  }

  if (idContainer) {
    identifier {
      value = idContainer[PSN]
      type {
        coding {
          system = "urn:centraxx"
          code = idContainer[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
        }
      }
    }
  }
  /*
  if (context.source[GENDER_TYPE]) {
    gender = mapGender(context.source[GENDER_TYPE] as GenderType)
  }
  */

  birthDate = normalizeDate(context.source[patientMasterDataAnonymous().birthdate().date()] as String)
  deceasedDateTime = "UNKNOWN" != context.source[patientMasterDataAnonymous().dateOfDeath().precision()] ?
      context.source[patientMasterDataAnonymous().dateOfDeath().date()] : null
  generalPractitioner {
    identifier {
      value = "P-2216-NAP"
    }
  }

}

static def mapGender(final GenderType genderType) {
  switch (genderType) {
    case GenderType.MALE:
      return "male"
    case GenderType.FEMALE:
      return "female"
    case GenderType.UNKNOWN:
      return "unknown"
    default:
      return "other"
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null // removes the time
}
