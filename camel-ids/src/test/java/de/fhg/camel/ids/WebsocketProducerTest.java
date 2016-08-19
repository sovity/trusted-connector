/**
 * 
 */
package de.fhg.camel.ids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.fhg.camel.ids.DefaultWebsocket;
import de.fhg.camel.ids.WebsocketConstants;
import de.fhg.camel.ids.WebsocketProducer;
import de.fhg.camel.ids.WebsocketStore;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class WebsocketProducerTest {
    
    private static final String MESSAGE = "MESSAGE";
    private static final String SESSION_KEY = "random-session-key";
    
    @Mock
    private Endpoint endpoint;

    @Mock
    private WebsocketStore store;
    
    @Mock
    private Session connection;
    
    @Mock
    private DefaultWebsocket defaultWebsocket1;
    
    @Mock
    private DefaultWebsocket defaultWebsocket2;

    @Mock
    private Exchange exchange;
    
    @Mock
    private Message inMessage;

    private IOException exception = new IOException("BAD NEWS EVERYONE!");

    private WebsocketProducer websocketProducer;
    
    private Collection<DefaultWebsocket> sockets;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        websocketProducer = new WebsocketProducer(endpoint, store);
        sockets = Arrays.asList(defaultWebsocket1, defaultWebsocket2);
    }

    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#process(org.apache.camel.Exchange)}.
     */
    @Test
    @Ignore
    public void testProcessSingleMessage() throws Exception {
        when(exchange.getIn()).thenReturn(inMessage);
        when(inMessage.getBody(String.class)).thenReturn(MESSAGE);
        when(inMessage.getHeader(WebsocketConstants.SEND_TO_ALL)).thenReturn(null);
        when(inMessage.getHeader(WebsocketConstants.CONNECTION_KEY, String.class)).thenReturn(SESSION_KEY);
        when(store.get(SESSION_KEY)).thenReturn(defaultWebsocket1);
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(true);

        websocketProducer.process(exchange);
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verify(exchange, times(1)).getIn();
        inOrder.verify(inMessage, times(1)).getBody(String.class);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.SEND_TO_ALL);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.CONNECTION_KEY, String.class);
        inOrder.verify(store, times(1)).get(SESSION_KEY);
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#process(org.apache.camel.Exchange)}.
     */
    @Test
    @Ignore
    public void testProcessSingleMessageWithException() throws Exception {
        when(exchange.getIn()).thenReturn(inMessage);
        when(inMessage.getBody(String.class)).thenReturn(MESSAGE);
        when(inMessage.getHeader(WebsocketConstants.SEND_TO_ALL)).thenReturn(false);
        when(inMessage.getHeader(WebsocketConstants.CONNECTION_KEY, String.class)).thenReturn(SESSION_KEY);
        when(store.get(SESSION_KEY)).thenReturn(defaultWebsocket1);
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(true);
        doThrow(exception).when(connection).getRemote().sendString(MESSAGE);

        try {
            websocketProducer.process(exchange);
            fail("Exception expected");
        }
        catch (IOException ioe) {
            assertEquals(exception, ioe);
        }
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verify(exchange, times(1)).getIn();
        inOrder.verify(inMessage, times(1)).getBody(String.class);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.SEND_TO_ALL);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.CONNECTION_KEY, String.class);
        inOrder.verify(store, times(1)).get(SESSION_KEY);
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#process(org.apache.camel.Exchange)}.
     */
    @Test
    @Ignore
    public void testProcessMultipleMessages() throws Exception {
        when(exchange.getIn()).thenReturn(inMessage);
        when(inMessage.getBody(String.class)).thenReturn(MESSAGE);
        when(inMessage.getHeader(WebsocketConstants.SEND_TO_ALL)).thenReturn(true);
        when(store.getAll()).thenReturn(sockets);
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(defaultWebsocket2.getConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(true);

        websocketProducer.process(exchange);
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verify(exchange, times(1)).getIn();
        inOrder.verify(inMessage, times(1)).getBody(String.class);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.SEND_TO_ALL);
        inOrder.verify(store, times(1)).getAll();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verify(defaultWebsocket2, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket2, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#process(org.apache.camel.Exchange)}.
     */
    @Test
    @Ignore
    public void testProcessMultipleMessagesWithExcpetion() throws Exception {
        when(exchange.getIn()).thenReturn(inMessage);
        when(inMessage.getBody(String.class)).thenReturn(MESSAGE);
        when(inMessage.getHeader(WebsocketConstants.SEND_TO_ALL)).thenReturn(true);
        when(store.getAll()).thenReturn(sockets);
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(defaultWebsocket2.getConnection()).thenReturn(connection);
        doThrow(exception).when(connection).getRemote().sendString(MESSAGE);
        when(connection.isOpen()).thenReturn(true);

        try {
            websocketProducer.process(exchange);
            fail("Exception expected");
        }
        catch (Exception e) {
            assertEquals(exception, e.getCause());
        }
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verify(exchange, times(1)).getIn();
        inOrder.verify(inMessage, times(1)).getBody(String.class);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.SEND_TO_ALL);
        inOrder.verify(store, times(1)).getAll();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verify(defaultWebsocket2, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket2, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#process(org.apache.camel.Exchange)}.
     */
    @Test
    @Ignore
    public void testProcessSingleMessageNoConnectionKey() throws Exception {
        when(exchange.getIn()).thenReturn(inMessage);
        when(inMessage.getBody(String.class)).thenReturn(MESSAGE);
        when(inMessage.getHeader(WebsocketConstants.SEND_TO_ALL)).thenReturn(false);
        when(inMessage.getHeader(WebsocketConstants.CONNECTION_KEY, String.class)).thenReturn(null);

        try {
            websocketProducer.process(exchange);
            fail("Exception expected");
        }
        catch (Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(e.getMessage());
            assertNull(e.getCause());
        }
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verify(exchange, times(1)).getIn();
        inOrder.verify(inMessage, times(1)).getBody(String.class);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.SEND_TO_ALL);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.CONNECTION_KEY, String.class);
        inOrder.verifyNoMoreInteractions();
    }



    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#sendMessage(de.fhg.camel.ids.DefaultWebsocket, java.lang.String)}.
     */
    @Test
    public void testSendMessage() throws Exception {
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(true);
        
        websocketProducer.sendMessage(defaultWebsocket1, MESSAGE);
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#sendMessage(de.fhg.camel.ids.DefaultWebsocket, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testSendMessageWebsocketIsNull() throws Exception {
        
        websocketProducer.sendMessage(null, MESSAGE);
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#sendMessage(de.fhg.camel.ids.DefaultWebsocket, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testSendMessageConnetionIsClosed() throws Exception {
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(false);
        
        websocketProducer.sendMessage(defaultWebsocket1, MESSAGE);
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#sendMessage(de.fhg.camel.ids.DefaultWebsocket, java.lang.String)}.
     */
    @Test
    public void testSendMessageWithException() throws Exception {
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(true);
        doThrow(exception).when(connection).getRemote().sendString(MESSAGE);

        try {
            websocketProducer.sendMessage(defaultWebsocket1, MESSAGE);
            fail("Exception expected");
        }
        catch (IOException ioe) {
            assertEquals(exception, ioe);
        }
        
        InOrder inOrder = inOrder(endpoint, store, connection, defaultWebsocket1, defaultWebsocket2, exchange, inMessage);
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#isSendToAllSet(Message)}.
     */
    @Test
    public void testIsSendToAllSet() {
        when(inMessage.getHeader(WebsocketConstants.SEND_TO_ALL)).thenReturn(true, false);
        assertTrue(websocketProducer.isSendToAllSet(inMessage));
        assertFalse(websocketProducer.isSendToAllSet(inMessage));
        InOrder inOrder = inOrder(inMessage);
        inOrder.verify(inMessage, times(2)).getHeader(WebsocketConstants.SEND_TO_ALL);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#isSendToAllSet(Message)}.
     */
    @Test
    public void testIsSendToAllSetHeaderNull() {
        when(inMessage.getHeader(WebsocketConstants.SEND_TO_ALL)).thenReturn(null);
        assertFalse(websocketProducer.isSendToAllSet(inMessage));
        InOrder inOrder = inOrder(inMessage);
        inOrder.verify(inMessage, times(1)).getHeader(WebsocketConstants.SEND_TO_ALL);
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#sendToAll(WebsocketStore, String)}.
     */
    @Test
    public void testSendToAll() throws Exception {
        when(store.getAll()).thenReturn(sockets);
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(defaultWebsocket2.getConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(true);

        websocketProducer.sendToAll(store, MESSAGE);
        
        InOrder inOrder = inOrder(store, connection, defaultWebsocket1, defaultWebsocket2);
        inOrder.verify(store, times(1)).getAll();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verify(defaultWebsocket2, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket2, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketProducer#sendToAll(WebsocketStore, String)}.
     */
    @Test
    public void testSendToAllWithExcpetion() throws Exception {
        when(store.getAll()).thenReturn(sockets);
        when(defaultWebsocket1.getConnection()).thenReturn(connection);
        when(defaultWebsocket2.getConnection()).thenReturn(connection);
        doThrow(exception).when(connection).getRemote().sendString(MESSAGE);
        when(connection.isOpen()).thenReturn(true);

        try {
            websocketProducer.sendToAll(store, MESSAGE);
            fail("Exception expected");
        }
        catch (Exception e) {
            assertEquals(exception, e.getCause());
        }
        
        InOrder inOrder = inOrder(store, connection, defaultWebsocket1, defaultWebsocket2);
        inOrder.verify(store, times(1)).getAll();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket1, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verify(defaultWebsocket2, times(1)).getConnection();
        inOrder.verify(connection, times(1)).isOpen();
        inOrder.verify(defaultWebsocket2, times(1)).getConnection();
        inOrder.verify(connection, times(1)).getRemote().sendString(MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
}
