/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

public class SpanFirstQueryBuilder extends AbstractQueryBuilder<SpanFirstQueryBuilder> implements SpanQueryBuilder<SpanFirstQueryBuilder>{

    public static final String NAME = "span_first";

    private final SpanQueryBuilder matchBuilder;

    private final int end;

    static final SpanFirstQueryBuilder SPAN_FIRST_QUERY_BUILDER = new SpanFirstQueryBuilder(null, -1);

    /**
     * Query that matches spans queries defined in <code>matchBuilder</code>
     * whose end position is less than or equal to <code>end</code>.
     * @param matchBuilder inner {@link SpanQueryBuilder}
     * @param end maximum end position of the match, needs to be positive
     * @throws IllegalArgumentException for negative <code>end</code> positions
     */
    public SpanFirstQueryBuilder(SpanQueryBuilder matchBuilder, int end) {
        this.matchBuilder = matchBuilder;
        this.end = end;
    }

    /**
     * @return the inner {@link SpanQueryBuilder} defined in this query
     */
    public SpanQueryBuilder matchBuilder() {
        return this.matchBuilder;
    }

    /**
     * @return maximum end position of the matching inner span query
     */
    public int end() {
        return this.end;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.field("match");
        matchBuilder.toXContent(builder, params);
        builder.field("end", end);
        printBoostAndQueryName(builder);
        builder.endObject();
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        Query innerSpanQuery = matchBuilder.toQuery(context);
        assert innerSpanQuery instanceof SpanQuery;
        return new SpanFirstQuery((SpanQuery) innerSpanQuery, end);
    }

    @Override
    public QueryValidationException validate() {
        QueryValidationException validationException = null;
        if (matchBuilder == null) {
            validationException = addValidationError("inner clause [match] cannot be null.", validationException);
        } else {
            validationException = validateInnerQuery(matchBuilder, validationException);
        }
        if (end < 0) {
            validationException = addValidationError("parameter [end] needs to be positive.", validationException);
        }
        return validationException;
    }

    @Override
    protected SpanFirstQueryBuilder doReadFrom(StreamInput in) throws IOException {
        SpanQueryBuilder matchBuilder = in.readNamedWriteable();
        int end = in.readInt();
        return new SpanFirstQueryBuilder(matchBuilder, end);
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeNamedWriteable(matchBuilder);
        out.writeInt(end);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(matchBuilder, end);
    }

    @Override
    protected boolean doEquals(SpanFirstQueryBuilder other) {
        return Objects.equals(matchBuilder, other.matchBuilder) &&
               Objects.equals(end, other.end);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
