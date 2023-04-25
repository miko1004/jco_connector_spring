package com.grandetree.jco_conntor_ex1.jco.server;

import com.sap.conn.jco.AbapClassException;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

public class FunctionHandler implements JCoServerFunctionHandler {
    @Override
    public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
    {
        serverCtx.setStateful(true);
        System.out.println("----------------------------------------------------------------");
        System.out.println("Called function   : "+function.getName());
        System.out.println("ConnectionId      : "+serverCtx.getConnectionID());
        System.out.println("SessionId         : "+serverCtx.getSessionID());
        System.out.println("Repository name   : "+serverCtx.getRepository().getName());
        System.out.println("Is in transaction : "+serverCtx.isInTransaction());
        System.out.println("TID               : "+serverCtx.getTID());
        System.out.println("Is stateful       : "+serverCtx.isStatefulSession());
        System.out.println("----------------------------------------------------------------");
        System.out.println("Gateway host      : "+serverCtx.getServer().getGatewayHost());
        System.out.println("Gateway service   : "+serverCtx.getServer().getGatewayService());
        System.out.println("Program ID        : "+serverCtx.getServer().getProgramID());
        System.out.println("----------------------------------------------------------------");
        System.out.println("Attributes        : ");
        System.out.println(serverCtx.getConnectionAttributes().toString());
        System.out.println("----------------------------------------------------------------");
        System.out.println("CPIC conversation ID: "+serverCtx.getConnectionAttributes().getCPICConversationID());
        System.out.println("----------------------------------------------------------------");
        System.out.println(function.toXML());
    }
}
