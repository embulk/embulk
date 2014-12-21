package org.embulk.exec;

public class PreviewedNoticeError
        extends Error
{
    private final PreviewResult previewResult;

    public PreviewedNoticeError(PreviewResult previewResult)
    {
        this.previewResult = previewResult;
    }

    public PreviewResult getPreviewResult()
    {
        return previewResult;
    }
}
