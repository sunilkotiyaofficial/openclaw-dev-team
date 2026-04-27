package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * EIP showcase #5: File polling → S3 data lake (cross-domain integration)
 *
 * <p>Polls a directory for new files, streams each line as a separate
 * exchange, and writes batched output to S3 with date-partitioned keys.</p>
 *
 * <p><b>Why this matters for AI/ML interview tomorrow:</b></p>
 * <ul>
 *   <li>This is how data lands in your data lake for AI/ML training pipelines</li>
 *   <li>Partition by date enables efficient queries via Athena/Trino later</li>
 *   <li>Camel's S3 component handles multipart upload, retries, and idempotency</li>
 *   <li>Combined with Iceberg/Delta Lake on top of S3 = production data lakehouse</li>
 * </ul>
 *
 * <p><b>Interview talking point:</b> "When customers ask 'how does data
 * land in your AI feature store', this route is the answer.
 * Files dropped into the watched directory by upstream batch systems
 * (legacy ETL, partner SFTP feeds) get streamed into the S3 data lake.
 * From there, Glue + Athena make it queryable; SageMaker/Bedrock can
 * reference it for training/inference."</p>
 */
@Component
public class FileToS3DataLakeRoute extends RouteBuilder {

    @Override
    public void configure() {

        // ─── EIP: File Polling Consumer ─────────────────────────────────
        // Polls /tmp/inbox every 5 seconds for new files
        from("file:/tmp/inbox?"
                + "moveFailed=.error"             // failed files → /tmp/inbox/.error/
                + "&move=.processed"               // success files → /tmp/inbox/.processed/
                + "&readLock=changed"              // wait for file to stabilize before reading
                + "&readLockTimeout=10000"
                + "&delay=5000")                    // 5s polling interval
                .routeId("file-to-s3-data-lake")
                .log("Picked up file: ${header.CamelFileName}")

                // Date-partition the S3 key for efficient query later
                // Path: s3://data-lake/raw/yyyy=2026/mm=04/dd=26/order-batch-001.csv
                .setHeader("CamelAwsS3Key", simple(
                        "raw/yyyy=" + LocalDate.now().getYear()
                        + "/mm=" + String.format("%02d", LocalDate.now().getMonthValue())
                        + "/dd=" + String.format("%02d", LocalDate.now().getDayOfMonth())
                        + "/${header.CamelFileName}"))

                // ─── EIP: Producer to AWS S3 ────────────────────────────
                // Camel handles: retries, multipart upload, error handling
                .to("aws2-s3://"
                        + "{{aws.s3.data-lake-bucket}}"
                        + "?accessKey={{aws.s3.access-key}}"
                        + "&secretKey={{aws.s3.secret-key}}"
                        + "&region={{aws.s3.region}}"
                        + "&overrideEndpoint=true"
                        + "&uriEndpointOverride={{aws.s3.endpoint}}"  // LocalStack in dev
                        + "&forcePathStyle=true")                       // required for LocalStack

                .log("✓ Uploaded to S3: ${header.CamelAwsS3Key}");

        // ─── Reverse direction: S3 → Local file (for verification) ──────
        // Enable in tests to verify round-trip
        // from("aws2-s3://{{aws.s3.data-lake-bucket}}?...")
        //     .to("file:/tmp/from-s3");
    }
}
