package projects.cxx.v2

import de.kairos.centraxx.common.types.sample.SampleKind
import de.kairos.centraxx.fhir.r4.utils.FhirUrls
import de.kairos.fhir.centraxx.metamodel.IdContainerType

import static de.kairos.fhir.centraxx.metamodel.AbstractEntity.ID
import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.ID_CONTAINER_TYPE
import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.PSN
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.COLD_ISCH_TIME
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.COLD_ISCH_TIME_DATE
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.ID_CONTAINER
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.PARENT
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SAMPLE_CATEGORY
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SAMPLE_KIND
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SECOND_PROCESSING
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SECOND_PROCESSING_DATE
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SPREC_FIXATION_TIME
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SPREC_FIXATION_TIME_DATE
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SPREC_POST_CENTRIFUGATION_DELAY
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SPREC_POST_CENTRIFUGATION_DELAY_DATE
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SPREC_PRE_CENTRIFUGATION_DELAY
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SPREC_PRE_CENTRIFUGATION_DELAY_DATE
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SPREC_PRIMARY_SAMPLE_CONTAINER
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SPREC_TISSUE_COLLECTION_TYPE
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.STOCK_PROCESSING
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.STOCK_PROCESSING_DATE
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.STOCK_TYPE
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.USE_SPREC
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.WARM_ISCH_TIME
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.WARM_ISCH_TIME_DATE
import static de.kairos.fhir.centraxx.metamodel.RootEntities.sample
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.ORGANISATION_UNIT

/**
 * Represented by a CXX AbstractSample
 * @author Mike Wähnert
 * @since v.1.7.0, CXX.v.3.17.2
 */
specimen {

  id = "Specimen/" + context.source[ID]


  final def idContainer = context.source[ID_CONTAINER]?.find {
    "SAMPLEID" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
  }

  final def idContainerExt = context.source[ID_CONTAINER]?.find {
    "EXTSAMPLEID" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
  }

  final def idContainerNUMSet = context.source[ID_CONTAINER]?.find {
    "NUM_EXT_SETID" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
  }

  if (idContainer) {
    identifier {
      value = idContainer[PSN]
      type {
        coding {
          system = "urn:centraxx"
          code = "EXTSAMPLEID"
        }
      }
    }
  }

  if (idContainerExt) {
    identifier {
      value = idContainerExt[PSN]
      type {
        coding {
          system = "urn:centraxx"
          code = "SAMPLEID"
        }
      }
    }
  }

  if (idContainerNUMSet) {
    identifier {
      value = idContainerNUMSet[PSN]
      type {
        coding {
          system = "urn:centraxx"
          code = "SETID"
        }
      }
    }
  }


  status = context.source[sample().restAmount().amount()] > 0 ? "available" : "unavailable"

  type {
    coding {
      system = "urn:centraxx"
      code = toHubType(context.source[sample().sampleType().code()])
    }
  }



  final def patIdContainer = context.source[sample().patientContainer().idContainer()]?.find {
    "LIMSPSN" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
  }

  if (patIdContainer) {
    subject {
      identifier {
        value = patIdContainer[PSN]
        type {
          coding {
            system = "urn:centraxx"
            code = patIdContainer[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
          }
        }
      }
    }
  }

  if (context.source[PARENT] != null) {
    parent {
      reference = "Specimen/" + context.source[sample().parent().id()]
    }
  }

  receivedTime {
    date = context.source[sample().receiptDate().date()]
  }

  collection {
    collectedDateTime {
      date = context.source[sample().samplingDate().date()]
      quantity {
        value = context.source[sample().initialAmount().amount()] as Number
        unit = context.source[sample().initialAmount().unit()]
        system = "urn:centraxx"
      }
    }
  }


  //TODO: Standard organization unit attached to sample
  // 2021-07-01 Extension noch nicht durch KAIROS umgesetzt
  // extension{
  //  url = "https://fhir.centraxx.de/extension/sample/sampleOrgunit"
  //  valueCoding{
  //    system = "https://fhir.centraxx.de/extension/sample/sampleOrgunit"
  // Hannover OE
  //    code = "P-2216-NAP"
  
  //  }
  //}
  //organization {
  //  identifier{
  //    value = "OrganisationUnit/" + context.source[sample().organisationUnit().id()]
  //    type {
  //      coding {
  //        system = "https://fhir.centraxx.de/extension/sample/sampleOrgunit"
  //        code = context.source[sample().organisationUnit().code()]
  //      }
  //    }
  //  }
  //}

  container {
    if (context.source[sample().receptable()]) {
      identifier {
        value = toHubContainer(context.source[sample().sampleType().code()])
        system = "urn:centraxx"
      }

      capacity {
        value = toHubContainerCapacity(context.source[sample().sampleType().code()])
        unit = toHubContainerCapacityUnit(context.source[sample().sampleType().code()])
        system = "urn:centraxx"
      }
    }

    specimenQuantity {
      value = context.source[sample().restAmount().amount()] as Number
      unit = toHubContainerCapacityUnit(context.source[sample().sampleType().code()])
      system = "urn:centraxx"
    } "urn:centraxx"
    }

    extension {
      url = FhirUrls.Extension.Sample.
              valueCoding {
                system =
      code = context.source[SAMPLE_CATEGORY]
    }
  }

  if (context.source[sample().repositionDate()]) {
    extension {
      url = FhirUrls.Extension.Sample.REPOSITION_DATE
      valueDateTime = context.source[sample().repositionDate().date()]
    }
  }

  if (context.source[sample().derivalDate()]) {
    extension {
      url = FhirUrls.Extension.Sample.DERIVAL_DATE
      valueDateTime = context.source[sample().derivalDate().date()]
    }
  }



  // SPREC Extensions
  extension {
    url = FhirUrls.Extension.SPREC
    extension {
      url = FhirUrls.Extension.Sprec.USE_SPREC
      valueBoolean = context.source[USE_SPREC]
    }

//    if (context.source["sprecCode"]) {
//      extension {
//        url = FhirUrls.Extension.Sprec.SPREC_CODE
//        valueCoding {
//          system = "https://doi.org/10.1089/bio.2017.0109"
//          code = context.source[sample().sprecCode()]
//        }
//      }
//    }

    //
    // SPREC TISSUE
    //
    if (SampleKind.TISSUE == context.source[SAMPLE_KIND] as SampleKind) {
      if (context.source[SPREC_TISSUE_COLLECTION_TYPE]) {
        extension {
          url = FhirUrls.Extension.Sprec.SPREC_TISSUE_COLLECTION_TYPE
          valueCoding {
            system = "urn:centraxx"
            code = context.source[sample().sprecTissueCollectionType().code()]
          }
        }
      }
      if (context.source[WARM_ISCH_TIME]) {
        extension {
          url = FhirUrls.Extension.Sprec.WARM_ISCH_TIME
          valueCoding {
            system = "urn:centraxx"
            code = context.source[sample().warmIschTime().code()]
          }
        }
      }
      if (context.source[WARM_ISCH_TIME_DATE]) {
        extension {
          url = FhirUrls.Extension.Sprec.WARM_ISCH_TIME_DATE
          valueDateTime = context.source[sample().warmIschTimeDate().date()]
        }
      }
      if (context.source[COLD_ISCH_TIME]) {
        extension {
          url = FhirUrls.Extension.Sprec.COLD_ISCH_TIME
          valueCoding {
            system = "urn:centraxx"
            code = context.source[sample().coldIschTime().code()]
          }
        }
      }
      if (context.source[COLD_ISCH_TIME_DATE]) {
        extension {
          url = FhirUrls.Extension.Sprec.COLD_ISCH_TIME_DATE
          valueDateTime = context.source[sample().coldIschTimeDate().date()]
        }
      }
      if (context.source[STOCK_TYPE]) {
        extension {
          url = FhirUrls.Extension.Sprec.STOCK_TYPE
          valueCoding {
            system = "urn:centraxx"
            code = context.source[sample().stockType().code()]
          }
        }
      }
      if (context.source[SPREC_FIXATION_TIME]) {
        extension {
          url = FhirUrls.Extension.Sprec.SPREC_FIXATION_TIME
          valueCoding {
            system = "urn:centraxx"
            code = context.source[sample().sprecFixationTime().code()]
          }
        }
      }
      if (context.source[SPREC_FIXATION_TIME_DATE]) {
        extension {
          url = FhirUrls.Extension.Sprec.SPREC_FIXATION_TIME_DATE
          valueDateTime = context.source[sample().sprecFixationTimeDate().date()]
        }
      }
    }

    //
    // SPREC LIQUID
    //
    if (SampleKind.LIQUID == context.source[SAMPLE_KIND] as SampleKind) {
      if (context.source[SPREC_PRIMARY_SAMPLE_CONTAINER]) {
        extension {
          url = FhirUrls.Extension.Sprec.SPREC_PRIMARY_SAMPLE_CONTAINER
          valueCoding {
            system = "urn:centraxx"
            code = toHubSprecPrimaryContainer(context.source[sample().sampleType().code()])
//			context.source[sample().sprecPrimarySampleContainer().code()]
          }
        }
      }
      if (context.source[SPREC_PRE_CENTRIFUGATION_DELAY]) {
        extension {
          url = FhirUrls.Extension.Sprec.SPREC_PRE_CENTRIFUGATION_DELAY
          valueCoding {
            system = "urn:centraxx"
            code = context.source[sample().sprecPreCentrifugationDelay().code()]
          }
        }
      }
      if (context.source[SPREC_PRE_CENTRIFUGATION_DELAY_DATE]) {
        extension {
          url = FhirUrls.Extension.Sprec.SPREC_PRE_CENTRIFUGATION_DELAY_DATE
          valueDateTime = context.source[sample().sprecPreCentrifugationDelayDate().date()]
        }
      }
      if (context.source[SPREC_POST_CENTRIFUGATION_DELAY]) {
        extension {
          url = FhirUrls.Extension.Sprec.SPREC_POST_CENTRIFUGATION_DELAY
          valueCoding {
            system = "urn:centraxx"
            code = context.source[sample().sprecPostCentrifugationDelay().code()]
          }
        }
      }
      if (context.source[SPREC_POST_CENTRIFUGATION_DELAY_DATE]) {
        extension {
          url = FhirUrls.Extension.Sprec.SPREC_POST_CENTRIFUGATION_DELAY_DATE
          valueDateTime = context.source[sample().sprecPostCentrifugationDelayDate().date()]
        }
      }
      if (context.source[STOCK_PROCESSING]) {
        extension {
          url = FhirUrls.Extension.Sprec.STOCK_PROCESSING
          valueCoding {
            system = "urn:centraxx"
            code = toNUMProcessing(context.source[sample().stockProcessing().code()] as String)
          }
        }
      }
      if (context.source[STOCK_PROCESSING_DATE]) {
        extension {
          url = FhirUrls.Extension.Sprec.STOCK_PROCESSING_DATE
          valueDateTime = context.source[sample().stockProcessingDate().date()]
        }
      }
      if (context.source[SECOND_PROCESSING]) {
        extension {
          url = FhirUrls.Extension.Sprec.SECOND_PROCESSING
          valueCoding {
            system = "urn:centraxx"
            code = toNUMProcessing(context.source[sample().secondProcessing().code()] as String)
          }
        }
      }
      if (context.source[SECOND_PROCESSING_DATE]) {
        extension {
          url = FhirUrls.Extension.Sprec.SECOND_PROCESSING_DATE
          valueDateTime = context.source[sample().secondProcessingDate().date()]
        }
      }
    }

    // Sample Location
    //   if (context.source[sample().sampleLocation()]) {
    //   extension {
    //   url = "https://fhir.centraxx.de/extension/sample/sampleLocation"
    // extension {
    //  url = "https://fhir.centraxx.de/extension/sample/sampleLocationPath"
    // valueString = "HUB --> HUB-FHIR" // context.source[sample().sampleLocation().locationPath()]
    // }
    //   extension {
    //    url = "https://fhir.centraxx.de/extension/sample/xPosition"
    //    valueInteger = context.source[sample().xPosition()] as Integer
    //}
    // extension {
    //  url = "https://fhir.centraxx.de/extension/sample/yPosition"
    // valueInteger = context.source[sample().yPosition()] as Integer
    //}
    //   }
    //}




  }
}

static String toHubContainerCapacityUnit(final Object sourceType) {
  switch (sourceType) {
    case "SAL":
      return ""
    default:
      return "ML"
  }
}

static String toHubContainerCapacity(final Object sourceType) {
  switch (sourceType) {
    case "SAL":
      return "1.0"
    case "NUM_pax":
      return "2.5"
    case "URN":
      return "8.5"
    default:
      return "7.5"
  }
}

static String toHubContainer(final Object sourceType) {
  switch (sourceType) {
    case "SER":
      return "StMono075"
    case "EDTAWB":
      return "StMono075"
    case "CIT":
      return "StMono075"
    case "NUM_pax":
      return "BDPax025"
    case "NUM_pbmc_edta":
      return "StMono075"
    case "NUM_speichel":
      return "StSali001"
    case "URN":
      return "StMono085"
    default:
      return sourceType
  }

}

static String toHubType(final Object sourceType) {
  switch (sourceType) {
    case "SER":
      return "BLD"
    case "EDTAWB":
      return "BLD"
    case "CIT":
      return "BLD"
    case "NUM_pax":
      return "BLD"
    case "NUM_pbmc_edta":
      return "BLD"
    case "NUM_speichel":
      return "SAL"
    case "URN":
      return "URN"
    default:
      return sourceType
  }
}

static String toHubSprecPrimaryContainer(final Object sourceType) {
  switch (sourceType) {
    case "SER":
      return "SST"
    case "EDTAWB":
      return "PED"
    case "CIT":
      return "SCI"
    case "NUM_pax":
      return "PAX"
    case "NUM_pbmc_edta":
      return "PED"
    case "NUM_speichel":
      return "SAL"
    case "URN":
      return "ZZZ(ppu)"
    default:
      return sourceType
  }
}

static String toNumType(final Object sourceType) {
  switch (sourceType) {
    case "BAL":
      return "NUM_bal"
    case "ZZZ(nab)":
      return "NUM_abstrich"
    case "ZZZ(pbm)":
      return "NUM_pbmc"
    case "SAL":
      return "NUM_speichel"
    case ["SPT", "SPT(ind)"]:
      return "NUM_sputum"
    case "ZZZ(usd)":
      return "NUM_urins"
    default:
      return sourceType
  }
}

static String toNUMProcessing(final String sourceProcessing) {
  if (sourceProcessing.startsWith("A"))
    return "Sprec-A"
  if (sourceProcessing.startsWith("B"))
    return "Sprec-B"
  if (sourceProcessing.startsWith("C"))
    return "Sprec-C"
  if (sourceProcessing.startsWith("D"))
    return "Sprec-D"
  if (sourceProcessing.startsWith("E"))
    return "Sprec-E"
  if (sourceProcessing.startsWith("F"))
    return "Sprec-F"
  if (sourceProcessing.startsWith("G"))
    return "Sprec-G"
  if (sourceProcessing.startsWith("H"))
    return "Sprec-H"
  if (sourceProcessing.startsWith("I"))
    return "Sprec-I"
  if (sourceProcessing.startsWith("J"))
    return "Sprec-J"
  if (sourceProcessing.startsWith("M"))
    return "Sprec-M"
  if (sourceProcessing.startsWith("N"))
    return "Sprec-N"
  if (sourceProcessing.startsWith("X"))
    return "Sprec-X"
  if (sourceProcessing.startsWith("Z"))
    return "Sprec-Z"
  else
    return sourceProcessing
}
