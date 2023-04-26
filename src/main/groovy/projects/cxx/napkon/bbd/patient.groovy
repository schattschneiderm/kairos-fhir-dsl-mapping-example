package projects.cxx.napkon.num_hgw

import de.kairos.fhir.centraxx.metamodel.IdContainerType
import org.hl7.fhir.r4.model.Attachment

import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.ID_CONTAINER_TYPE
import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.PSN
import static de.kairos.fhir.centraxx.metamodel.RootEntities.patientMasterDataAnonymous
import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyMember
/**
 * Represented by a CXX PatientMasterDataAnonymous
 * Intended to be used with PUT (createOrUpdateByIdType) methods, because samples will be assigned by a logical fhir patient id reference.
 * @author Mario Schattschneider

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
                    code = "ID_NUM_DRKS00023742_NAPKON"
                }
            }
        }
    }

    birthDate = normalizeDate(context.source[patientMasterDataAnonymous().birthdate().date()] as String)
    deceasedDateTime = "UNKNOWN" != context.source[patientMasterDataAnonymous().dateOfDeath().precision()] ?
            context.source[patientMasterDataAnonymous().dateOfDeath().date()] : null

    generalPractitioner {
        identifier {
            value = "DILB"
        }
    }

}

static String normalizeDate(final String dateTimeString) {
    return dateTimeString != null ? dateTimeString.substring(0, 10) : null // removes the time
}
