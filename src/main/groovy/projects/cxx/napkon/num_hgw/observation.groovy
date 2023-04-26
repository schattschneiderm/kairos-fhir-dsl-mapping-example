package projects.cxx.napkon.num_hgw

import de.kairos.centraxx.fhir.r4.utils.FhirUrls
import de.kairos.fhir.centraxx.metamodel.IdContainer
import de.kairos.fhir.centraxx.metamodel.IdContainerType
import de.kairos.fhir.centraxx.metamodel.LaborFindingLaborValue
import de.kairos.fhir.centraxx.metamodel.LaborMapping
import de.kairos.fhir.centraxx.metamodel.LaborValue
import de.kairos.fhir.centraxx.metamodel.LaborValueNumeric
import de.kairos.fhir.centraxx.metamodel.PrecisionDate
import de.kairos.fhir.centraxx.metamodel.SampleIdContainer
import de.kairos.fhir.centraxx.metamodel.Unity
import de.kairos.fhir.centraxx.metamodel.UsageEntry
import de.kairos.fhir.centraxx.metamodel.enums.LaborMappingType
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.LaborFindingLaborValue.LABOR_VALUE
import static de.kairos.fhir.centraxx.metamodel.RecordedValue.AMOUNT_VALUE
import static de.kairos.fhir.centraxx.metamodel.RecordedValue.BOOLEAN_VALUE
import static de.kairos.fhir.centraxx.metamodel.RecordedValue.DATE_VALUE
import static de.kairos.fhir.centraxx.metamodel.RecordedValue.NUMERIC_VALUE
import static de.kairos.fhir.centraxx.metamodel.RecordedValue.STRING_VALUE
import static de.kairos.fhir.centraxx.metamodel.RecordedValue.TIME_VALUE
import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborMapping
/**
 * Represented by a CXX LaborMapping
 * @author Mario Schattschneider
 * @since v.1.19.0, CXX.v.3.18.2.3
 *
 * The mapping transforms specimen from the NUM Greifswald system to the IRB Greifswald system.
 * Script to extract measurement results that contain only simple data types and single / multiple selections from controlled vocabulary.
 * Based on the assumption that the measurement profiles (LaborMethods), measurement parameters (LaborValues) and the associated controlled vocabulary are
 * defined with the same codes in both CXX instances. In this case, only one mapping to the oid of the value list in the target system is required
 * for the import.
 */
observation {
  final String patContainerIdType = "MPI"

  def validMappings = [LaborMappingType.SAMPLELABORMAPPING, LaborMappingType.PATIENTLABORMAPPING]

  println("Mappings" + context.source[laborMapping().mappingType() ])
  println("Code" + context.source[laborMapping().laborFinding().laborMethod().code()])

  def isNUMRelevant = ["DZHKF", "NUM_REGISTRY_LIQUOR", "NUM_REGISTRY_URN", "NUM_REGISTRY_STOOL"].contains(context.source[laborMapping().laborFinding().laborMethod().code()])
  println("isNUMRelevant" + Boolean.toString(isNUMRelevant))
  println("isValid" + Boolean.toString(validMappings.contains(context.source[laborMapping().mappingType()] as LaborMappingType)))

  if (!(validMappings.contains(context.source[laborMapping().mappingType()] as LaborMappingType) && isNUMRelevant)) {
    return
  }

  id = "Observation/" + context.source[laborMapping().id()]

  extension {
    url = FhirUrls.Extension.UPDATE_WITH_OVERWRITE
    valueBoolean = false
  }

  status = Observation.ObservationStatus.UNKNOWN

  code {
    coding {
      system = "urn:centraxx"
      code = context.source[laborMapping().laborFinding().shortName()] as String
    }
  }

  final def patIdContainer = context.source[laborMapping().relatedPatient().idContainer()]?.find {
    "LIMSPSN" == it[IdContainer.ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
  }

  if (patIdContainer) {
    subject {
      identifier {
        value = patIdContainer[IdContainer.PSN]
        type {
          coding {
            system = "urn:centraxx"
            code = patContainerIdType
          }
        }
      }
    }
  }

  if (context.source[laborMapping().sample()] != null) {
    // Reference by identifier SampleId, because parent MasterSample might already exists in the target system
    // The external sample id of HUB is provided as sample id to DZHK.
    final def extSampleId = context.source[laborMapping().sample().idContainer()]?.find { final def entry ->
      "SAMPLEID" == entry[SampleIdContainer.ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
    }

    if (extSampleId) {
      specimen {
        identifier {
          type {
            coding {
              code = "NUM_SAMPLEID"
            }
          }
          value = extSampleId[SampleIdContainer.PSN]
        }
      }
    }
  }

  effectiveDateTime = context.source[laborMapping().laborFinding().findingDate().date()]

  method {
    coding {
      system = "urn:centraxx"
      version = context.source[laborMapping().laborFinding().laborMethod().version()]
      code = context.source[laborMapping().laborFinding().laborMethod().code()] as String
    }
  }

  context.source[laborMapping().laborFinding().laborFindingLaborValues()].each { final def labFinLabVal ->
    component {
      code {
        coding {
          system = "urn:centraxx"
          code = toLocalLabFinLabValCode(labFinLabVal[LABOR_VALUE][LaborValue.CODE] as String)
        }
      }
      if (labFinLabVal[NUMERIC_VALUE]) {
        valueQuantity {
          value = labFinLabVal[NUMERIC_VALUE]
          unit = labFinLabVal[LABOR_VALUE]?.getAt(LaborValueNumeric.UNIT)?.getAt(Unity.CODE) as String
        }
      }

      if (labFinLabVal[STRING_VALUE]) {
        valueString(labFinLabVal[STRING_VALUE] as String)
      }

      if (labFinLabVal[AMOUNT_VALUE]) {
        valueQuantity {
          value = labFinLabVal[AMOUNT_VALUE]
        }
      }

      if (labFinLabVal[DATE_VALUE]) {
        valueDateTime {
          date = labFinLabVal[DATE_VALUE]?.getAt(PrecisionDate.DATE)
        }
      }

      if (labFinLabVal[TIME_VALUE]) {
        valueTime(labFinLabVal[TIME_VALUE] as String)
      }

      if (labFinLabVal[BOOLEAN_VALUE]) {
        valueBoolean(labFinLabVal[BOOLEAN_VALUE] as Boolean)
      }


      if (labFinLabVal[LaborFindingLaborValue.MULTI_VALUE]){
        valueCodeableConcept {
          labFinLabVal[LaborFindingLaborValue.MULTI_VALUE].each {final def entry ->
            coding {
              system = "urn:centraxx:CodeSystem/UsageEntry-" + entry[UsageEntry.ID]
              code = toLocalUsageEntryCode(entry[UsageEntry.CODE] as String)
            }
          }
        }
      }
    }
  }
}

static String toLocalLabFinLabValCode(String code) {
  String result = code
  switch (code) {
    case "QMBL":
      result = "PROBE_BLUTIG"
    case "QMCL":
      result = "PROBE_TRUEB"
  }
  return result
}

static String toLocalUsageEntryCode(String code) {
  String result = code

  switch (code) {
    case "UNKN":
      result = "UNBEKANNT"
    case "UN":
      result = "NICHT_ERHOBEN"
    case "YES":
      result = "JA"
    case "NO":
      result = "NEIN"
  }
  return  result
}
