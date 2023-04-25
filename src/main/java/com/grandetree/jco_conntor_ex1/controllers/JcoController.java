package com.grandetree.jco_conntor_ex1.controllers;

import com.grandetree.jco_conntor_ex1.jco.server.FunctionHandler;
import com.grandetree.jco_conntor_ex1.services.JcoService;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

@RestController
@Slf4j
public class JcoController {

    @Autowired
    private JcoService jcoService;
    JCoServer server = null;

    @PostMapping("/createDestinationFile")
    public HashMap<String, Object> createDestination() {
        try {
            FileWriter clientFileWriter = new FileWriter("ABAP_AS1.jcoDestination");
            FileWriter serverFileWriter = new FileWriter("EXT_SERVER.jcoServer");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        return resultMap;
    }

    @GetMapping("/start")
    public HashMap<String, Object> startJcoServer() throws JCoException {

        HashMap<String, Object> resultMap = new HashMap<String, Object>();

        if(server == null) {
            server = JCoServerFactory.getServer("EXT_SERVER");

            FunctionHandler handler = new FunctionHandler();
            DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
            factory.registerGenericHandler(handler);
            server.setCallHandlerFactory(factory);
            server.start();
        }
        log.info("RFC Server Started...");

        resultMap.put("message", "RFC Server Started");
        return resultMap;
    }

    @GetMapping("/")
    public String hello(){
        return "hello";
    }

    @GetMapping("/stop")
    public HashMap<String, Object> stopServer() throws InterruptedException {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        server.stop();
        for (int i=0; i<60 && server.getState()!=JCoServerState.STOPPED; i++)
            Thread.sleep(1000);
        resultMap.put("message", "server stopped.");
        return resultMap;
    }

}
