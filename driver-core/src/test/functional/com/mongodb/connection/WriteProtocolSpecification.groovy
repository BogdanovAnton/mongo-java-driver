/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.mongodb.connection

import com.mongodb.DuplicateKeyException
import com.mongodb.OperationFunctionalSpecification
import com.mongodb.bulk.InsertRequest
import com.mongodb.connection.netty.NettyStreamFactory
import com.mongodb.internal.validator.NoOpFieldNameValidator
import org.bson.BsonBinary
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import org.bson.codecs.BsonDocumentCodec
import spock.lang.Shared

import static com.mongodb.ClusterFixture.getCredentialList
import static com.mongodb.ClusterFixture.getPrimary
import static com.mongodb.ClusterFixture.getSslSettings
import static com.mongodb.WriteConcern.ACKNOWLEDGED
import static com.mongodb.WriteConcern.UNACKNOWLEDGED

class WriteProtocolSpecification extends OperationFunctionalSpecification {
    @Shared
    InternalStreamConnection connection;

    def setupSpec() {
        connection = new InternalStreamConnectionFactory(new NettyStreamFactory(SocketSettings.builder().build(), getSslSettings()),
                                                         getCredentialList(), new NoOpConnectionListener())
                .create(new ServerId(new ClusterId(), getPrimary()))
        connection.open();
    }

    def cleanupSpec() {
        connection?.close()
    }

    def 'should ignore write errors on unacknowledged inserts'() {
        given:
        def documentOne = new BsonDocument('_id', new BsonInt32(1))
        def documentTwo = new BsonDocument('_id', new BsonInt32(2))
        def documentThree = new BsonDocument('_id', new BsonInt32(3))
        def documentFour = new BsonDocument('_id', new BsonInt32(4))

        def insertRequest = [new InsertRequest(documentOne), new InsertRequest(documentTwo),
                             new InsertRequest(documentThree), new InsertRequest(documentFour)]
        def protocol = new InsertProtocol(getNamespace(), true, UNACKNOWLEDGED, insertRequest)

        getCollectionHelper().insertDocuments(documentOne)

        when:
        protocol.execute(connection)

        then:
        getCollectionHelper().find(new BsonDocumentCodec()) == [documentOne]

        cleanup:
        // force acknowledgement
        new CommandProtocol(getDatabaseName(), new BsonDocument('drop', new BsonString(getCollectionName())),
                            new NoOpFieldNameValidator(), new BsonDocumentCodec()).execute(connection)
    }

    def 'should report write errors on acknowledged inserts'() {
        given:
        def documentOne = new BsonDocument('_id', new BsonInt32(1))
        def documentTwo = new BsonDocument('_id', new BsonInt32(2))
        def documentThree = new BsonDocument('_id', new BsonInt32(3))
        def documentFour = new BsonDocument('_id', new BsonInt32(4))

        def insertRequest = [new InsertRequest(documentOne), new InsertRequest(documentTwo),
                             new InsertRequest(documentThree), new InsertRequest(documentFour)]
        def protocol = new InsertProtocol(getNamespace(), true, ACKNOWLEDGED, insertRequest)

        getCollectionHelper().insertDocuments(documentOne)

        when:
        protocol.execute(connection)

        then:
        thrown(DuplicateKeyException)
        getCollectionHelper().count() == 1
    }

    def 'should ignore write errors on split unacknowledged inserts'() {
        given:
        def binary = new BsonBinary(new byte[15000000])
        def documentOne = new BsonDocument('_id', new BsonInt32(1)).append('b', binary)
        def documentTwo = new BsonDocument('_id', new BsonInt32(2)).append('b', binary)
        def documentThree = new BsonDocument('_id', new BsonInt32(3)).append('b', binary)
        def documentFour = new BsonDocument('_id', new BsonInt32(4)).append('b', binary)

        def insertRequest = [new InsertRequest(documentOne), new InsertRequest(documentTwo),
                             new InsertRequest(documentThree), new InsertRequest(documentFour)]
        def protocol = new InsertProtocol(getNamespace(), true, UNACKNOWLEDGED, insertRequest)

        getCollectionHelper().insertDocuments(documentOne)

        when:
        protocol.execute(connection)

        then:
        getCollectionHelper().count() == 1

        cleanup:
        // force acknowledgement
        new CommandProtocol(getDatabaseName(), new BsonDocument('drop', new BsonString(getCollectionName())),
                            new NoOpFieldNameValidator(), new BsonDocumentCodec()).execute(connection)
    }

    def 'should report write errors on split acknowledged inserts'() {
        given:
        def binary = new BsonBinary(new byte[15000000])
        def documentOne = new BsonDocument('_id', new BsonInt32(1)).append('b', binary)
        def documentTwo = new BsonDocument('_id', new BsonInt32(2)).append('b', binary)
        def documentThree = new BsonDocument('_id', new BsonInt32(3)).append('b', binary)
        def documentFour = new BsonDocument('_id', new BsonInt32(4)).append('b', binary)

        def insertRequest = [new InsertRequest(documentOne), new InsertRequest(documentTwo),
                             new InsertRequest(documentThree), new InsertRequest(documentFour)]
        def protocol = new InsertProtocol(getNamespace(), true, ACKNOWLEDGED, insertRequest)

        getCollectionHelper().insertDocuments(documentOne)

        when:
        protocol.execute(connection)

        then:
        thrown(DuplicateKeyException)
        getCollectionHelper().count() == 1
    }
}
