package org.quickload.standards;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;

public class CsvRecordReader implements Iterable<List<String>>
{
    private final CsvTokenizer tokenizer;

    public CsvRecordReader(CsvTokenizer tokenizer)
    {
        this.tokenizer = tokenizer;
    }

    @Override
    public Iterator<List<String>> iterator() {
        return new Ite(this.tokenizer);
    }

    private static class Ite implements Iterator<List<String>>
    {
        private final CsvTokenizer tokenizer;
        private List<String> record;

        Ite(CsvTokenizer tokenizer)
        {
            this.tokenizer = tokenizer;
        }

        @Override
        public boolean hasNext()
        {
            if (record != null) {
                return true;
            }

            if (!tokenizer.hasNextRecord()) {
                return false;
            }

            while (tokenizer.hasNextToken()) {
                if (record == null) {
                    record = new ArrayList<>();
                }
                CsvTokenizer.Token token = checkNotNull(tokenizer.nextToken(), "Tokenizer should not return null.");
                //System.out.println("token: "+token);
                if (!token.isEmptyColumnString()) {
                    record.add(token.getColumn());
                } else {
                    record.add(token.isQuoted() ? "" : null);
                }
            }

            return record != null;
        }

        @Override
        public List<String> next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            // deep copy
            List<String> r = new ArrayList<String>();
            r.addAll(record);
            record = null;
            return r;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
