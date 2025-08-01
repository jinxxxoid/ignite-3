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

// <auto-generated/>
namespace Apache.Ignite
{
    using System;

    public static partial class ErrorGroups
    {
        /// <summary>
        /// Gets the group name by code.
        /// </summary>
        /// <param name="groupCode">Group code.</param>
        /// <returns>Group name.</returns>
        public static string GetGroupName(int groupCode) => groupCode switch
        {
            Common.GroupCode => Common.GroupName,
            Table.GroupCode => Table.GroupName,
            Client.GroupCode => Client.GroupName,
            Sql.GroupCode => Sql.GroupName,
            MetaStorage.GroupCode => MetaStorage.GroupName,
            Index.GroupCode => Index.GroupName,
            Transactions.GroupCode => Transactions.GroupName,
            Replicator.GroupCode => Replicator.GroupName,
            Storage.GroupCode => Storage.GroupName,
            DistributionZones.GroupCode => DistributionZones.GroupName,
            Network.GroupCode => Network.GroupName,
            NodeConfiguration.GroupCode => NodeConfiguration.GroupName,
            CodeDeployment.GroupCode => CodeDeployment.GroupName,
            GarbageCollector.GroupCode => GarbageCollector.GroupName,
            Authentication.GroupCode => Authentication.GroupName,
            Compute.GroupCode => Compute.GroupName,
            Catalog.GroupCode => Catalog.GroupName,
            PlacementDriver.GroupCode => PlacementDriver.GroupName,
            CriticalWorkers.GroupCode => CriticalWorkers.GroupName,
            DisasterRecovery.GroupCode => DisasterRecovery.GroupName,
            Embedded.GroupCode => Embedded.GroupName,
            Marshalling.GroupCode => Marshalling.GroupName,
            Rest.GroupCode => Rest.GroupName,
            CommonConfiguration.GroupCode => CommonConfiguration.GroupName,

            _ => UnknownGroupName
        };

        /// <summary>
        /// Gets the group error prefix by code.
        /// </summary>
        /// <param name="groupCode">Group code.</param>
        /// <returns>Group error prefix.</returns>
        public static string GetErrorPrefix(int groupCode) => groupCode switch
        {
            Common.GroupCode => Common.ErrorPrefix,
            Table.GroupCode => Table.ErrorPrefix,
            Client.GroupCode => Client.ErrorPrefix,
            Sql.GroupCode => Sql.ErrorPrefix,
            MetaStorage.GroupCode => MetaStorage.ErrorPrefix,
            Index.GroupCode => Index.ErrorPrefix,
            Transactions.GroupCode => Transactions.ErrorPrefix,
            Replicator.GroupCode => Replicator.ErrorPrefix,
            Storage.GroupCode => Storage.ErrorPrefix,
            DistributionZones.GroupCode => DistributionZones.ErrorPrefix,
            Network.GroupCode => Network.ErrorPrefix,
            NodeConfiguration.GroupCode => NodeConfiguration.ErrorPrefix,
            CodeDeployment.GroupCode => CodeDeployment.ErrorPrefix,
            GarbageCollector.GroupCode => GarbageCollector.ErrorPrefix,
            Authentication.GroupCode => Authentication.ErrorPrefix,
            Compute.GroupCode => Compute.ErrorPrefix,
            Catalog.GroupCode => Catalog.ErrorPrefix,
            PlacementDriver.GroupCode => PlacementDriver.ErrorPrefix,
            CriticalWorkers.GroupCode => CriticalWorkers.ErrorPrefix,
            DisasterRecovery.GroupCode => DisasterRecovery.ErrorPrefix,
            Embedded.GroupCode => Embedded.ErrorPrefix,
            Marshalling.GroupCode => Marshalling.ErrorPrefix,
            Rest.GroupCode => Rest.ErrorPrefix,
            CommonConfiguration.GroupCode => CommonConfiguration.ErrorPrefix,

            _ => UnknownGroupName
        };

        /// <summary> Common errors. </summary>
        public static class Common
        {
            /// <summary> Common group code. </summary>
            public const short GroupCode = 1;

            /// <summary> Common group name. </summary>
            public const String GroupName = "CMN";

            /// <summary> Common error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> NodeStopping error. </summary>
            public const int NodeStopping = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> ComponentNotStarted error. </summary>
            public const int ComponentNotStarted = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> IllegalArgument error. </summary>
            public const int IllegalArgument = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> SslConfiguration error. </summary>
            public const int SslConfiguration = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> NodeLeft error. </summary>
            public const int NodeLeft = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> CursorAlreadyClosed error. </summary>
            public const int CursorAlreadyClosed = (GroupCode << 16) | (6 & 0xFFFF);

            /// <summary> ResourceClosing error. </summary>
            public const int ResourceClosing = (GroupCode << 16) | (7 & 0xFFFF);

            /// <summary> UserObjectSerialization error. </summary>
            public const int UserObjectSerialization = (GroupCode << 16) | (8 & 0xFFFF);

            /// <summary> NullableValue error. </summary>
            public const int NullableValue = (GroupCode << 16) | (9 & 0xFFFF);

            /// <summary> Internal error. </summary>
            public const int Internal = (GroupCode << 16) | (65535 & 0xFFFF);
        }

        /// <summary> Table errors. </summary>
        public static class Table
        {
            /// <summary> Table group code. </summary>
            public const short GroupCode = 2;

            /// <summary> Table group name. </summary>
            public const String GroupName = "TBL";

            /// <summary> Table error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> TableAlreadyExists error. </summary>
            public const int TableAlreadyExists = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> TableNotFound error. </summary>
            public const int TableNotFound = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> ColumnAlreadyExists error. </summary>
            public const int ColumnAlreadyExists = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> ColumnNotFound error. </summary>
            public const int ColumnNotFound = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> SchemaVersionMismatch error. </summary>
            public const int SchemaVersionMismatch = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> UnsupportedPartitionType error. </summary>
            public const int UnsupportedPartitionType = (GroupCode << 16) | (6 & 0xFFFF);
        }

        /// <summary> Client errors. </summary>
        public static class Client
        {
            /// <summary> Client group code. </summary>
            public const short GroupCode = 3;

            /// <summary> Client group name. </summary>
            public const String GroupName = "CLIENT";

            /// <summary> Client error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> Connection error. </summary>
            public const int Connection = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> Protocol error. </summary>
            public const int Protocol = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> ProtocolCompatibility error. </summary>
            public const int ProtocolCompatibility = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> TableIdNotFound error. </summary>
            public const int TableIdNotFound = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> Configuration error. </summary>
            public const int Configuration = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> ClusterIdMismatch error. </summary>
            public const int ClusterIdMismatch = (GroupCode << 16) | (6 & 0xFFFF);

            /// <summary> ClientSslConfiguration error. </summary>
            public const int ClientSslConfiguration = (GroupCode << 16) | (7 & 0xFFFF);

            /// <summary> HandshakeHeader error. </summary>
            public const int HandshakeHeader = (GroupCode << 16) | (8 & 0xFFFF);

            /// <summary> ServerToClientRequest error. </summary>
            public const int ServerToClientRequest = (GroupCode << 16) | (9 & 0xFFFF);
        }

        /// <summary> Sql errors. </summary>
        public static class Sql
        {
            /// <summary> Sql group code. </summary>
            public const short GroupCode = 4;

            /// <summary> Sql group name. </summary>
            public const String GroupName = "SQL";

            /// <summary> Sql error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> QueryNoResultSet error. </summary>
            public const int QueryNoResultSet = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> SchemaNotFound error. </summary>
            public const int SchemaNotFound = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> StmtParse error. </summary>
            public const int StmtParse = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> StmtValidation error. </summary>
            public const int StmtValidation = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> ConstraintViolation error. </summary>
            public const int ConstraintViolation = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> ExecutionCancelled error. </summary>
            public const int ExecutionCancelled = (GroupCode << 16) | (6 & 0xFFFF);

            /// <summary> Runtime error. </summary>
            public const int Runtime = (GroupCode << 16) | (7 & 0xFFFF);

            /// <summary> Mapping error. </summary>
            public const int Mapping = (GroupCode << 16) | (8 & 0xFFFF);

            /// <summary> TxControlInsideExternalTx error. </summary>
            public const int TxControlInsideExternalTx = (GroupCode << 16) | (9 & 0xFFFF);
        }

        /// <summary> MetaStorage errors. </summary>
        public static class MetaStorage
        {
            /// <summary> MetaStorage group code. </summary>
            public const short GroupCode = 5;

            /// <summary> MetaStorage group name. </summary>
            public const String GroupName = "META";

            /// <summary> MetaStorage error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> StartingStorage error. </summary>
            public const int StartingStorage = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> RestoringStorage error. </summary>
            public const int RestoringStorage = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> Compaction error. </summary>
            public const int Compaction = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> OpExecution error. </summary>
            public const int OpExecution = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> OpExecutionTimeout error. </summary>
            public const int OpExecutionTimeout = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> Compacted error. </summary>
            public const int Compacted = (GroupCode << 16) | (6 & 0xFFFF);

            /// <summary> Diverged error. </summary>
            public const int Diverged = (GroupCode << 16) | (7 & 0xFFFF);
        }

        /// <summary> Index errors. </summary>
        public static class Index
        {
            /// <summary> Index group code. </summary>
            public const short GroupCode = 6;

            /// <summary> Index group name. </summary>
            public const String GroupName = "IDX";

            /// <summary> Index error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> IndexNotFound error. </summary>
            public const int IndexNotFound = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> IndexAlreadyExists error. </summary>
            public const int IndexAlreadyExists = (GroupCode << 16) | (2 & 0xFFFF);
        }

        /// <summary> Transactions errors. </summary>
        public static class Transactions
        {
            /// <summary> Transactions group code. </summary>
            public const short GroupCode = 7;

            /// <summary> Transactions group name. </summary>
            public const String GroupName = "TX";

            /// <summary> Transactions error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> TxStateStorage error. </summary>
            public const int TxStateStorage = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> TxStateStorageStopped error. </summary>
            public const int TxStateStorageStopped = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> TxUnexpectedState error. </summary>
            public const int TxUnexpectedState = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> AcquireLock error. </summary>
            public const int AcquireLock = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> AcquireLockTimeout error. </summary>
            public const int AcquireLockTimeout = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> TxCommit error. </summary>
            public const int TxCommit = (GroupCode << 16) | (6 & 0xFFFF);

            /// <summary> TxRollback error. </summary>
            public const int TxRollback = (GroupCode << 16) | (7 & 0xFFFF);

            /// <summary> TxFailedReadWriteOperation error. </summary>
            public const int TxFailedReadWriteOperation = (GroupCode << 16) | (8 & 0xFFFF);

            /// <summary> TxStateStorageRebalance error. </summary>
            public const int TxStateStorageRebalance = (GroupCode << 16) | (9 & 0xFFFF);

            /// <summary> TxReadOnlyTooOld error. </summary>
            public const int TxReadOnlyTooOld = (GroupCode << 16) | (10 & 0xFFFF);

            /// <summary> TxIncompatibleSchema error. </summary>
            public const int TxIncompatibleSchema = (GroupCode << 16) | (11 & 0xFFFF);

            /// <summary> TxPrimaryReplicaExpired error. </summary>
            public const int TxPrimaryReplicaExpired = (GroupCode << 16) | (12 & 0xFFFF);

            /// <summary> TxAlreadyFinished error. </summary>
            public const int TxAlreadyFinished = (GroupCode << 16) | (13 & 0xFFFF);

            /// <summary> TxStaleOperation error. </summary>
            public const int TxStaleOperation = (GroupCode << 16) | (14 & 0xFFFF);

            /// <summary> TxStaleReadOnlyOperation error. </summary>
            public const int TxStaleReadOnlyOperation = (GroupCode << 16) | (15 & 0xFFFF);

            /// <summary> TxAlreadyFinishedWithTimeout error. </summary>
            public const int TxAlreadyFinishedWithTimeout = (GroupCode << 16) | (16 & 0xFFFF);
        }

        /// <summary> Replicator errors. </summary>
        public static class Replicator
        {
            /// <summary> Replicator group code. </summary>
            public const short GroupCode = 8;

            /// <summary> Replicator group name. </summary>
            public const String GroupName = "REP";

            /// <summary> Replicator error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> ReplicaCommon error. </summary>
            public const int ReplicaCommon = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> ReplicaIsAlreadyStarted error. </summary>
            public const int ReplicaIsAlreadyStarted = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> ReplicaTimeout error. </summary>
            public const int ReplicaTimeout = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> ReplicaUnsupportedRequest error. </summary>
            public const int ReplicaUnsupportedRequest = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> ReplicaUnavailable error. </summary>
            public const int ReplicaUnavailable = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> ReplicaMiss error. </summary>
            public const int ReplicaMiss = (GroupCode << 16) | (6 & 0xFFFF);

            /// <summary> CursorClose error. </summary>
            public const int CursorClose = (GroupCode << 16) | (7 & 0xFFFF);

            /// <summary> ReplicaStopping error. </summary>
            public const int ReplicaStopping = (GroupCode << 16) | (8 & 0xFFFF);

            /// <summary> GroupOverloaded error. </summary>
            public const int GroupOverloaded = (GroupCode << 16) | (9 & 0xFFFF);
        }

        /// <summary> Storage errors. </summary>
        public static class Storage
        {
            /// <summary> Storage group code. </summary>
            public const short GroupCode = 9;

            /// <summary> Storage group name. </summary>
            public const String GroupName = "STORAGE";

            /// <summary> Storage error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> IndexNotBuilt error. </summary>
            public const int IndexNotBuilt = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> StorageCorrupted error. </summary>
            public const int StorageCorrupted = (GroupCode << 16) | (2 & 0xFFFF);
        }

        /// <summary> DistributionZones errors. </summary>
        public static class DistributionZones
        {
            /// <summary> DistributionZones group code. </summary>
            public const short GroupCode = 10;

            /// <summary> DistributionZones group name. </summary>
            public const String GroupName = "DISTRZONES";

            /// <summary> DistributionZones error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> ZoneNotFound error. </summary>
            public const int ZoneNotFound = (GroupCode << 16) | (1 & 0xFFFF);
        }

        /// <summary> Network errors. </summary>
        public static class Network
        {
            /// <summary> Network group code. </summary>
            public const short GroupCode = 11;

            /// <summary> Network group name. </summary>
            public const String GroupName = "NETWORK";

            /// <summary> Network error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> UnresolvableConsistentId error. </summary>
            public const int UnresolvableConsistentId = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> Bind error. </summary>
            public const int Bind = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> FileTransfer error. </summary>
            public const int FileTransfer = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> FileValidation error. </summary>
            public const int FileValidation = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> RecipientLeft error. </summary>
            public const int RecipientLeft = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> AddressUnresolved error. </summary>
            public const int AddressUnresolved = (GroupCode << 16) | (6 & 0xFFFF);

            /// <summary> PortInUse is obsolete. Use Bind instead. </summary>
            [Obsolete]
            public const int PortInUse = Bind;
        }

        /// <summary> NodeConfiguration errors. </summary>
        public static class NodeConfiguration
        {
            /// <summary> NodeConfiguration group code. </summary>
            public const short GroupCode = 12;

            /// <summary> NodeConfiguration group name. </summary>
            public const String GroupName = "NODECFG";

            /// <summary> NodeConfiguration error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> ConfigRead error. </summary>
            public const int ConfigRead = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> ConfigFileCreate error. </summary>
            public const int ConfigFileCreate = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> ConfigWrite error. </summary>
            public const int ConfigWrite = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> ConfigParse error. </summary>
            public const int ConfigParse = (GroupCode << 16) | (4 & 0xFFFF);
        }

        /// <summary> CodeDeployment errors. </summary>
        public static class CodeDeployment
        {
            /// <summary> CodeDeployment group code. </summary>
            public const short GroupCode = 13;

            /// <summary> CodeDeployment group name. </summary>
            public const String GroupName = "CODEDEPLOY";

            /// <summary> CodeDeployment error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> UnitNotFound error. </summary>
            public const int UnitNotFound = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> UnitAlreadyExists error. </summary>
            public const int UnitAlreadyExists = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> UnitContentRead error. </summary>
            public const int UnitContentRead = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> UnitUnavailable error. </summary>
            public const int UnitUnavailable = (GroupCode << 16) | (4 & 0xFFFF);
        }

        /// <summary> GarbageCollector errors. </summary>
        public static class GarbageCollector
        {
            /// <summary> GarbageCollector group code. </summary>
            public const short GroupCode = 14;

            /// <summary> GarbageCollector group name. </summary>
            public const String GroupName = "GC";

            /// <summary> GarbageCollector error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> Closed error. </summary>
            public const int Closed = (GroupCode << 16) | (1 & 0xFFFF);
        }

        /// <summary> Authentication errors. </summary>
        public static class Authentication
        {
            /// <summary> Authentication group code. </summary>
            public const short GroupCode = 15;

            /// <summary> Authentication group name. </summary>
            public const String GroupName = "AUTHENTICATION";

            /// <summary> Authentication error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> UnsupportedAuthenticationType error. </summary>
            public const int UnsupportedAuthenticationType = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> InvalidCredentials error. </summary>
            public const int InvalidCredentials = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> BasicProvider error. </summary>
            public const int BasicProvider = (GroupCode << 16) | (3 & 0xFFFF);
        }

        /// <summary> Compute errors. </summary>
        public static class Compute
        {
            /// <summary> Compute group code. </summary>
            public const short GroupCode = 16;

            /// <summary> Compute group name. </summary>
            public const String GroupName = "COMPUTE";

            /// <summary> Compute error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> ClassPath error. </summary>
            public const int ClassPath = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> ClassLoader error. </summary>
            public const int ClassLoader = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> ClassInitialization error. </summary>
            public const int ClassInitialization = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> QueueOverflow error. </summary>
            public const int QueueOverflow = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> ComputeJobStatusTransition error. </summary>
            public const int ComputeJobStatusTransition = (GroupCode << 16) | (5 & 0xFFFF);

            /// <summary> Cancelling error. </summary>
            public const int Cancelling = (GroupCode << 16) | (6 & 0xFFFF);

            /// <summary> ResultNotFound error. </summary>
            public const int ResultNotFound = (GroupCode << 16) | (7 & 0xFFFF);

            /// <summary> FailToGetJobState error. </summary>
            public const int FailToGetJobState = (GroupCode << 16) | (8 & 0xFFFF);

            /// <summary> ComputeJobFailed error. </summary>
            public const int ComputeJobFailed = (GroupCode << 16) | (9 & 0xFFFF);

            /// <summary> PrimaryReplicaResolve error. </summary>
            public const int PrimaryReplicaResolve = (GroupCode << 16) | (10 & 0xFFFF);

            /// <summary> ChangeJobPriority error. </summary>
            public const int ChangeJobPriority = (GroupCode << 16) | (11 & 0xFFFF);

            /// <summary> NodeNotFound error. </summary>
            public const int NodeNotFound = (GroupCode << 16) | (12 & 0xFFFF);

            /// <summary> MarshallingTypeMismatch error. </summary>
            public const int MarshallingTypeMismatch = (GroupCode << 16) | (13 & 0xFFFF);

            /// <summary> ComputeJobCancelled error. </summary>
            public const int ComputeJobCancelled = (GroupCode << 16) | (14 & 0xFFFF);

            /// <summary> ComputePlatformExecutor error. </summary>
            public const int ComputePlatformExecutor = (GroupCode << 16) | (15 & 0xFFFF);
        }

        /// <summary> Catalog errors. </summary>
        public static class Catalog
        {
            /// <summary> Catalog group code. </summary>
            public const short GroupCode = 17;

            /// <summary> Catalog group name. </summary>
            public const String GroupName = "CATALOG";

            /// <summary> Catalog error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> Validation error. </summary>
            public const int Validation = (GroupCode << 16) | (1 & 0xFFFF);
        }

        /// <summary> PlacementDriver errors. </summary>
        public static class PlacementDriver
        {
            /// <summary> PlacementDriver group code. </summary>
            public const short GroupCode = 18;

            /// <summary> PlacementDriver group name. </summary>
            public const String GroupName = "PLACEMENTDRIVER";

            /// <summary> PlacementDriver error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> PrimaryReplicaAwaitTimeout error. </summary>
            public const int PrimaryReplicaAwaitTimeout = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> PrimaryReplicaAwait error. </summary>
            public const int PrimaryReplicaAwait = (GroupCode << 16) | (2 & 0xFFFF);
        }

        /// <summary> CriticalWorkers errors. </summary>
        public static class CriticalWorkers
        {
            /// <summary> CriticalWorkers group code. </summary>
            public const short GroupCode = 19;

            /// <summary> CriticalWorkers group name. </summary>
            public const String GroupName = "WORKERS";

            /// <summary> CriticalWorkers error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> SystemWorkerBlocked error. </summary>
            public const int SystemWorkerBlocked = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> SystemCriticalOperationTimeout error. </summary>
            public const int SystemCriticalOperationTimeout = (GroupCode << 16) | (2 & 0xFFFF);
        }

        /// <summary> DisasterRecovery errors. </summary>
        public static class DisasterRecovery
        {
            /// <summary> DisasterRecovery group code. </summary>
            public const short GroupCode = 20;

            /// <summary> DisasterRecovery group name. </summary>
            public const String GroupName = "RECOVERY";

            /// <summary> DisasterRecovery error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> IllegalPartitionId error. </summary>
            public const int IllegalPartitionId = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> NodesNotFound error. </summary>
            public const int NodesNotFound = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> PartitionState error. </summary>
            public const int PartitionState = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> ClusterNotIdle error. </summary>
            public const int ClusterNotIdle = (GroupCode << 16) | (4 & 0xFFFF);

            /// <summary> RestartWithCleanUp error. </summary>
            public const int RestartWithCleanUp = (GroupCode << 16) | (5 & 0xFFFF);
        }

        /// <summary> Embedded errors. </summary>
        public static class Embedded
        {
            /// <summary> Embedded group code. </summary>
            public const short GroupCode = 21;

            /// <summary> Embedded group name. </summary>
            public const String GroupName = "EMBEDDED";

            /// <summary> Embedded error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> ClusterNotInitialized error. </summary>
            public const int ClusterNotInitialized = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> ClusterInitFailed error. </summary>
            public const int ClusterInitFailed = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> NodeNotStarted error. </summary>
            public const int NodeNotStarted = (GroupCode << 16) | (3 & 0xFFFF);

            /// <summary> NodeStart error. </summary>
            public const int NodeStart = (GroupCode << 16) | (4 & 0xFFFF);
        }

        /// <summary> Marshalling errors. </summary>
        public static class Marshalling
        {
            /// <summary> Marshalling group code. </summary>
            public const short GroupCode = 22;

            /// <summary> Marshalling group name. </summary>
            public const String GroupName = "MARSHALLING";

            /// <summary> Marshalling error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> Common error. </summary>
            public const int Common = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> UnsupportedObjectType error. </summary>
            public const int UnsupportedObjectType = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> Unmarshalling error. </summary>
            public const int Unmarshalling = (GroupCode << 16) | (3 & 0xFFFF);
        }

        /// <summary> Rest errors. </summary>
        public static class Rest
        {
            /// <summary> Rest group code. </summary>
            public const short GroupCode = 23;

            /// <summary> Rest group name. </summary>
            public const String GroupName = "REST";

            /// <summary> Rest error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> ClusterNotInit error. </summary>
            public const int ClusterNotInit = (GroupCode << 16) | (1 & 0xFFFF);
        }

        /// <summary> CommonConfiguration errors. </summary>
        public static class CommonConfiguration
        {
            /// <summary> CommonConfiguration group code. </summary>
            public const short GroupCode = 24;

            /// <summary> CommonConfiguration group name. </summary>
            public const String GroupName = "COMMONCFG";

            /// <summary> CommonConfiguration error prefix. </summary>
            public const String ErrorPrefix = "IGN";

            /// <summary> ConfigurationApply error. </summary>
            public const int ConfigurationApply = (GroupCode << 16) | (1 & 0xFFFF);

            /// <summary> ConfigurationParse error. </summary>
            public const int ConfigurationParse = (GroupCode << 16) | (2 & 0xFFFF);

            /// <summary> ConfigurationValidation error. </summary>
            public const int ConfigurationValidation = (GroupCode << 16) | (3 & 0xFFFF);
        }
    }
}
