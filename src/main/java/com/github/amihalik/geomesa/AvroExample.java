package com.github.amihalik.geomesa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.geomesa.features.avro.AvroDataFileWriter;
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes;
import org.locationtech.geomesa.utils.interop.WKTUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class AvroExample {

    static SimpleFeature createNewFeature() {

        SimpleFeatureType simpleFeatureType = SimpleFeatureTypes.createType("MyFeatureType", "*Where:Point:srid=4326");
        Object[] NO_VALUES = {};

        String id = "Observation.1";
        SimpleFeature simpleFeature = SimpleFeatureBuilder.build(simpleFeatureType, NO_VALUES, id);

        simpleFeature.setAttribute("Where", WKTUtils.read("POINT( 1 1 )"));

        return simpleFeature;
    }

    public static byte[] serializeSimpleFeature(SimpleFeature sf) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AvroDataFileWriter writer = new AvroDataFileWriter(baos, sf.getFeatureType(), -1);
        writer.append(sf);
        writer.close();
        return baos.toByteArray();
    }

    public static String deserializeWithAvro(byte[] data) throws IOException {
        final GenericData genericData = GenericData.get();
        try (final InputStream in = new ByteArrayInputStream(data);
                final DataFileStream<GenericRecord> reader = new DataFileStream<>(in, new GenericDatumReader<GenericRecord>())) {

            System.out.println("Schema :: " + reader.getSchema());

            GenericRecord currRecord = null;
            if (reader.hasNext()) {
                currRecord = reader.next();
            }

            return genericData.toString(currRecord);

        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("  ==  Testing Feature Without Userdata == ");
        SimpleFeature featureWithoutUserdata = createNewFeature();
        System.out.println("Feature :: " + featureWithoutUserdata);

        byte[] featureData = serializeSimpleFeature(featureWithoutUserdata);
        System.out.println("Size :: " + featureData.length);

        String deserializedFeature = deserializeWithAvro(featureData);
        System.out.println("deserializedFeature :: " + deserializedFeature);

        
        
        System.out.println();
        System.out.println("  ==  Testing Feature *With* Userdata == ");
        SimpleFeature featureWithUserdata = createNewFeature();
        featureWithUserdata.getUserData().put("MyKey", "MyValue");

        System.out.println("Feature :: " + featureWithUserdata);

        featureData = serializeSimpleFeature(featureWithUserdata);
        System.out.println("Size :: " + featureData.length);

        try {
            deserializedFeature = deserializeWithAvro(featureData);
            System.out.println("deserializedFeature :: " + deserializedFeature);
        } catch (AvroRuntimeException e) {
            System.out.println(e.getMessage());
        }

    }

}
