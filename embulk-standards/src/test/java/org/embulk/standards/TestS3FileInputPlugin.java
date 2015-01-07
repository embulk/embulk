package org.embulk.standards;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableList;

public class TestS3FileInputPlugin
{
    @Test
    public void listS3FilesByPrefix()
    {
        // AWSS3Client returns list1 for the first iteration and list2 next.
        List<S3ObjectSummary> list1 = ImmutableList.<S3ObjectSummary> of(bucket("in/", 0), bucket("in/file/", 0),
                bucket("in/file/sample.csv.gz", 12345));
        List<S3ObjectSummary> list2 = ImmutableList.<S3ObjectSummary> of(bucket("sample2.csv.gz", 0));
        ObjectListing ol = Mockito.mock(ObjectListing.class);

        Mockito.doReturn(list1).doReturn(list2).when(ol).getObjectSummaries();
        AmazonS3Client client = Mockito.mock(AmazonS3Client.class);
        Mockito.doReturn(ol).when(client).listObjects(Mockito.any(ListObjectsRequest.class));
        Mockito.doReturn("in/file/").doReturn(null).when(ol).getNextMarker();

        // It counts only size != 0 files.
        assertEquals(1, S3FileInputPlugin.listS3FilesByPrefix(client, "bucketName", "prefix").size());
    }

    private S3ObjectSummary bucket(String key, long size)
    {
        S3ObjectSummary bucket = new S3ObjectSummary();
        bucket.setKey(key);
        bucket.setSize(size);
        return bucket;
    }
}
