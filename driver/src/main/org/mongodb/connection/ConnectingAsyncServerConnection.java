/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
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
 */

package org.mongodb.connection;

import org.bson.ByteBuf;

import java.util.List;

import static org.mongodb.assertions.Assertions.notNull;

class ConnectingAsyncServerConnection implements AsyncServerConnection {
    private final AsyncConnection connection;
    private final ServerDescription serverDescription;

    ConnectingAsyncServerConnection(final AsyncConnection connection) {
        this.connection = notNull("connection", connection);
        this.serverDescription = ServerDescription.builder().state(ServerConnectionState.Connecting).type(ServerType.Unknown)
                .address(connection.getServerAddress()).build();
    }

    @Override
    public void sendMessage(final List<ByteBuf> byteBuffers, final SingleResultCallback<Void> callback) {
        connection.sendMessage(byteBuffers, callback);
    }

    @Override
    public void sendAndReceiveMessage(final List<ByteBuf> byteBuffers, final ResponseSettings responseSettings,
                                      final SingleResultCallback<ResponseBuffers> callback) {
        connection.sendAndReceiveMessage(byteBuffers, responseSettings, callback);
    }

    @Override
    public void receiveMessage(final ResponseSettings responseSettings, final SingleResultCallback<ResponseBuffers> callback) {
        connection.receiveMessage(responseSettings, callback);
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public boolean isClosed() {
        return connection.isClosed();
    }

    @Override
    public ServerAddress getServerAddress() {
        return connection.getServerAddress();
    }

    @Override
    public ServerDescription getDescription() {
        return serverDescription;
    }
}