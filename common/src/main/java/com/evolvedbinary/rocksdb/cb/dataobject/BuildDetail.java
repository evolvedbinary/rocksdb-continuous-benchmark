package com.evolvedbinary.rocksdb.cb.dataobject;

import com.evolvedbinary.rocksdb.cb.common.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Arrays;

import static com.evolvedbinary.rocksdb.cb.common.Buf.Buf;
import static java.nio.charset.StandardCharsets.UTF_8;

public class BuildDetail extends AbstractDataObject {

    private BuildDetailType buildDetailType;
    private Compression detailCompression;
    private Encoding detailEncoding;
    private byte[] detail;

    public BuildDetail() {
    }

    public BuildDetail(final BuildDetailType buildDetailType, final Compression detailCompression, final Encoding detailEncoding, final byte[] detail) {
        this.buildDetailType = buildDetailType;
        this.detailCompression = detailCompression;
        this.detailEncoding = detailEncoding;
        this.detail = detail;
    }

    public static BuildDetail forException(final Exception e) {
        return new BuildDetail(BuildDetailType.EXCEPTION_MESSAGE, Compression.NONE, Encoding.NONE, e.getMessage().getBytes(UTF_8));
    }

    public static BuildDetail forStdOut(final byte[] stdOutData) {
        return new BuildDetail(BuildDetailType.STDOUT_LOG, Compression.ZSTD, Encoding.BASE64, stdOutData);
    }

    public static BuildDetail forStdErr(final byte[] stdErrData) {
        return new BuildDetail(BuildDetailType.STDERR_LOG, Compression.ZSTD, Encoding.BASE64, stdErrData);
    }

    public BuildDetailType getBuildDetailType() {
        return buildDetailType;
    }

    public byte[] getDetail() {
        return detail;
    }

    void serializeFields(final JsonGenerator generator) throws IOException {
        generator.writeStringField("type", buildDetailType.name());
        generator.writeStringField("detailCompression", detailCompression.name());
        generator.writeStringField("detailEncoding", detailEncoding.name());

        // compress then encode
        final Buf compressedDetail = CompressionUtil.compress(Buf(detail), detailCompression);
        final Buf compressedAndEncodedDetail = EncodingUtil.encode(compressedDetail, detailEncoding);

        generator.writeStringField("detail", new String(compressedAndEncodedDetail.data, compressedAndEncodedDetail.offset, compressedAndEncodedDetail.length, UTF_8));
    }

    @Override
    BuildDetail deserializeFields(final JsonParser parser, JsonToken token) throws IOException {

        // new data fields
        BuildDetailType buildDetailType1 = null;
        Compression detailCompression1 = null;
        Encoding detailEncoding1 = null;
        byte[] compressedAndEncodedDetail1 = null;

        while (true) {
            token = parser.nextToken();
            if (token == null || token == JsonToken.END_OBJECT) {
                break;  // EOL
            }
            if (token == JsonToken.START_OBJECT) {
                throw new IOException("Unexpected Start object: " + token);
            }

            if (token == JsonToken.FIELD_NAME) {
                final String fieldName = parser.getCurrentName();

                // move to field value
                token = parser.nextToken();
                if (token != JsonToken.VALUE_STRING) {
                    throw new IOException("Expected field string value, but found: " + token);
                }

                if (fieldName.equals("type")) {
                    buildDetailType1 = BuildDetailType.valueOf(parser.getValueAsString());
                } else if (fieldName.equals("detailCompression")) {
                    detailCompression1 = Compression.valueOf(parser.getValueAsString());
                } else if (fieldName.equals("detailEncoding")) {
                    detailEncoding1 = Encoding.valueOf(parser.getValueAsString());
                } else if (fieldName.equals("detail")) {
                    compressedAndEncodedDetail1 = parser.getValueAsString().getBytes(UTF_8);
                }
            }
        }

        this.buildDetailType = buildDetailType1;
        this.detailCompression = detailCompression1;
        this.detailEncoding = detailEncoding1;

        // decode then decompress
        final byte[] decodedCompressedDetail = EncodingUtil.decode(compressedAndEncodedDetail1, detailEncoding);
        final Buf uncompressedDetail = CompressionUtil.decompress(Buf(decodedCompressedDetail), detailCompression);

        this.detail = Arrays.copyOfRange(uncompressedDetail.data, uncompressedDetail.offset, uncompressedDetail.offset + uncompressedDetail.length);

        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final BuildDetail that = (BuildDetail) o;

        if (buildDetailType != that.buildDetailType) return false;
        if (detailCompression != that.detailCompression) return false;
        if (detailEncoding != that.detailEncoding) return false;
        return Arrays.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
        int result = buildDetailType.hashCode();
        result = 31 * result + detailCompression.hashCode();
        result = 31 * result + detailEncoding.hashCode();
        result = 31 * result + Arrays.hashCode(detail);
        return result;
    }
}
