/*
 * Licensed to Metamarkets Group Inc. (Metamarkets) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Metamarkets licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.druid.timeline.partition;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.druid.data.input.InputRow;
import io.druid.data.input.Rows;

import java.util.List;

public class HashBasedNumberedShardSpec extends NumberedShardSpec
{
  private static final HashFunction hashFunction = Hashing.murmur3_32();
  private final ObjectMapper jsonMapper;

  @JsonCreator
  public HashBasedNumberedShardSpec(
      @JsonProperty("partitionNum") int partitionNum,
      @JsonProperty("partitions") int partitions,
      @JacksonInject ObjectMapper jsonMapper
  )
  {
    super(partitionNum, partitions);
    this.jsonMapper = jsonMapper;
  }

  @Override
  public boolean isInChunk(long timestamp, InputRow inputRow)
  {
    return (((long) hash(timestamp, inputRow)) - getPartitionNum()) % getPartitions() == 0;
  }

  protected int hash(long timestamp, InputRow inputRow)
  {
    final List<Object> groupKey = Rows.toGroupKey(timestamp, inputRow);
    try {
      return hashFunction.hashBytes(jsonMapper.writeValueAsBytes(groupKey)).asInt();
    }
    catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString()
  {
    return "HashBasedNumberedShardSpec{" +
           "partitionNum=" + getPartitionNum() +
           ", partitions=" + getPartitions() +
           '}';
  }

  @Override
  public ShardSpecLookup getLookup(final List<ShardSpec> shardSpecs)
  {
    return new ShardSpecLookup()
    {
      @Override
      public ShardSpec getShardSpec(long timestamp, InputRow row)
      {
        int index = Math.abs(hash(timestamp, row) % getPartitions());
        return shardSpecs.get(index);
      }
    };
  }
}
