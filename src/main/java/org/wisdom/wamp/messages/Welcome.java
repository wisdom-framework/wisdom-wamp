/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.wamp.messages;

import org.wisdom.wamp.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * The WAMP Welcome message.
 * http://wamp.ws/spec/#welcome_message
 */
public class Welcome extends Message {
    final String clientID;

    public Welcome(String session) {
        this.clientID = session;
    }

    @Override
    public MessageType getType() {
        return MessageType.WELCOME;
    }

    @Override
    public List<Object> toList() {
        List<Object> res = new ArrayList<>();
        res.add(getType().code());
        res.add(this.clientID);
        res.add(Constants.WAMP_PROTOCOL_VERSION);
        res.add(Constants.WAMP_SERVER_VERSION);
        return res;
    }

}
