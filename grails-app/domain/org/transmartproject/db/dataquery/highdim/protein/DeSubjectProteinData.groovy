package org.transmartproject.db.dataquery.highdim.protein

import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

class DeSubjectProteinData implements Serializable {

    BigDecimal intensity

    // irrelevant...
    //String     trialName
    //String     component
    //String     timepoint
    //BigDecimal patientId
    //String     subjectId
    //String     geneSymbol
    //Long       geneId
    //BigDecimal NValue
    //BigDecimal meanIntensity
    //BigDecimal stddevIntensity
    //BigDecimal medianIntensity
    BigDecimal zscore
    //BigDecimal logIntensity

    DeProteinAnnotation jAnnotation

    static belongsTo = [
            assay:      DeSubjectSampleMapping,
            annotation: DeProteinAnnotation,
    ]

    static mapping = {
        table schema:    'deapp'
        id    composite: ['assay', 'annotation']

        assay      column: 'assay_id'
        annotation column: 'protein_annotation_id'

        // this is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        jAnnotation column: 'protein_annotation_id', updateable: false, insertable: false
        version false
    }

    static constraints = {
        intensity nullable: true

        // irrelevant:
        //trialName       nullable: true, maxSize: 15
        //component       nullable: true, maxSize: 200
        //patientId       nullable: true
        //subjectId       nullable: true, maxSize: 10
        //geneSymbol      nullable: true, maxSize: 100
        //geneId          nullable: true
        //timepoint       nullable: true, maxSize: 20
        //'NValue'        nullable: true
        //meanIntensity   nullable: true
        //stddevIntensity nullable: true
        //medianIntensity nullable: true
        zscore          nullable: true
        //logIntensity    nullable: true
    }
}
