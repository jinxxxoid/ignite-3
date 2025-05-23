/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace Apache.Ignite.Internal.Linq;

using Ignite.Table;
using Remotion.Linq;

/// <summary>
/// Internal queryable interface.
/// </summary>
internal interface IIgniteQueryableInternal
{
    /// <summary>
    /// Gets the table name.
    /// </summary>
    QualifiedName TableName { get; }

    /// <summary>
    /// Gets the provider.
    /// </summary>
    IgniteQueryProvider Provider { get; }

    /// <summary>
    /// Gets the query model.
    /// </summary>
    /// <returns>Query model.</returns>
    QueryModel GetQueryModel();

    /// <summary>
    /// Gets the query data.
    /// </summary>
    /// <returns>Query data.</returns>
    QueryData GetQueryData();
}
