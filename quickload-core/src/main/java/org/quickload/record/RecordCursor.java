package org.quickload.record;

public class RecordCursor
{
    private final PageAllocator allocator;
    private final int[] columnOffsets;
    private final int payloadOffset;

    private Page page;
    private int position;
    private int rowSize;
    private final byte[] nullBitSet;

    RecordCursor(PageAllocator allocator, Schema schema)
    {
        this.allocator = allocator;
        this.columnOffsets = Page.columnOffsets(schema);
        this.payloadOffset = Page.payloadOffset(schema);
        this.nullBitSet = new byte[Page.nullBitSetSize(schema)];
    }

    public void reset(Page page)
    {
        if (this.page != null) {
            allocator.releasePage(page);
            this.page = null;
        }
        this.page = page;
        this.position = 0;
        this.rowSize = 0;
    }

    public boolean next()
    {
        if (page == null) {
            return false;
        }

        position += rowSize;
        if (position < page.length()) {
            rowSize = page.getInt(position);
            page.getBytes(position + 4, nullBitSet, 0, nullBitSet.length);
            return true;
        }

        allocator.releasePage(page);
        page = null;

        return false;
    }

    public Page getPage()
    {
        return page;
    }

    public boolean isNull(int columnIndex)
    {
        return (nullBitSet[columnIndex >>> 3] & (1 << (columnIndex & 7))) != 0;
    }

    public int getFixedLengthPosition(int columnIndex)
    {
        return position + columnOffsets[columnIndex];
    }

    public int getVariableLengthIndex(int columnIndex)
    {
        return page.getInt(position + columnOffsets[columnIndex]);
    }
}
