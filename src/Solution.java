import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;

public class Solution {
    private static final String PLEASE = "Please enter the file path\nFor examle d:\\Base.accdb";
    private static final String DRIVER = "net.ucanaccess.jdbc.UcanaccessDriver";
    private static final String UCANACCESS = "jdbc:ucanaccess://";
    private static final String QUERY = "SELECT * FROM ASUXML";
    private static final String KPP = "KPP";
    private static final String BUSINESSPARTNERS = "BusinessPartners";
    private static final String ACCOUNTNUMBER = "AccountNumber";
    private static final String BILLACCOUNTS = "BillAccounts";
    private static final String LETTERCODE = "LetterCode";
    private static final String CURRENCY = "Currency";
    private static final String RESULTDIR = "\\Partner";

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String filePath = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        //database path
        System.out.println(PLEASE);
        try {
            filePath = reader.readLine();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                reader.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        // registering Oracle JDBC driver class
        try {
            Class.forName(DRIVER);
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }

        //Opening database connection
        try {
            String dbUrl = UCANACCESS + filePath;

            connection = DriverManager.getConnection(dbUrl);
            statement = connection.createStatement();

            //Executing SQL
            resultSet = statement.executeQuery(QUERY);

            System.out.println();

            while (resultSet.next()){
                int id = resultSet.getInt(1);
                DataSet dataSet = parse(resultSet.getSQLXML(2));
                createXml(dataSet,Paths.get(filePath).getParent().toString() + RESULTDIR, id);
            }
        }catch (SQLException e){

        }finally {
            try {
                if(connection != null){
                    resultSet.close();
                    statement.close();
                    connection.close();
                }
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    //take the necessary data from xml
    private static DataSet parse(SQLXML xml){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        DataSet dataSet = new DataSet();

        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(xml.getBinaryStream());
            document.getDocumentElement().normalize();

            //get KPP
            NodeList nodeList = document.getElementsByTagName(KPP);

            ArrayList<String> kpp = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++){
                if (nodeList.item(i).getParentNode().getNodeName().equals(KPP) &&
                nodeList.item(i).getParentNode().getParentNode().getNodeName().equals(BUSINESSPARTNERS)){
                    kpp.add(nodeList.item(i).getChildNodes().item(0).getNodeValue());
                }
            }

            //get AccountNumber
            nodeList = document.getElementsByTagName(ACCOUNTNUMBER);
            ArrayList<String> accountNumber = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++){
                if (nodeList.item(i).getParentNode().getNodeName().equals(BILLACCOUNTS)){
                    accountNumber.add(nodeList.item(i).getChildNodes().item(0).getNodeValue());
                }
            }

            //get Currency
            nodeList = document.getElementsByTagName(LETTERCODE);
            ArrayList<String> currency = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++){
                if (nodeList.item(i).getParentNode().getNodeName().equals(CURRENCY)){
                    currency.add(nodeList.item(i).getChildNodes().item(0).getNodeValue());
                }
            }

            dataSet.setKpp(kpp);
            dataSet.setAccountNumber(accountNumber);
            dataSet.setCurrency(currency);
        }catch (Exception e){
            e.printStackTrace();
        }
        return dataSet;
    }

    private static void createXml(DataSet dataSet, String dir, int id){
        //directory creation
        File resultDir = new File(dir);

        if (!resultDir.exists()){
            try {
                resultDir.mkdir();
            }catch (SecurityException e){
                e.printStackTrace();
            }
        }

        //xml creation
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Element rootElement = document.createElement("Transmission");
            document.appendChild(rootElement);
            Element body = document.createElement("TransmissionBody");
            rootElement.appendChild(body);
            Element gLog = document.createElement("GLogXMLElement");
            body.appendChild(gLog);
            Element location = document.createElement("Location");
            gLog.appendChild(location);

            makeTreeOfData(location, dataSet, document);

            TransformerFactory transformerFactory = TransformerFactory.newDefaultInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            document.setXmlStandalone(true);
            DOMSource source = new DOMSource(document);
            StreamResult file = new StreamResult(new File(dir + "\\" + RESULTDIR + "_" + id + ".xml"));
            transformer.transform(source,file);
        }catch (ParserConfigurationException e){
            e.printStackTrace();
        }catch (TransformerConfigurationException e){
            e.printStackTrace();
        }catch (TransformerException e){
            e.printStackTrace();
        }
    }

    private static void makeTreeOfData(Element parent, DataSet dataSet, Document document){
        if (dataSet.getKpp() != null && !dataSet.getKpp().isEmpty()){
            makeSubTree(parent, dataSet.getKpp(), document, "КПП_");
        }
        if (dataSet.getAccountNumber() != null && !dataSet.getAccountNumber().isEmpty()){
            makeSubTree(parent, dataSet.getAccountNumber(), document, "СЧЕТ_");
        }
        if (dataSet.getCurrency() != null && !dataSet.getCurrency().isEmpty()){
            makeSubTree(parent, dataSet.getCurrency(), document, "ВАЛЮТА_");
        }
    }

    private static void makeSubTree(Element parent, ArrayList<String> list, Document doc, String xidName){
        for (int i = 0; i < list.size(); i++){
            Element locationRefnum = doc.createElement("LocationRefnum");
            parent.appendChild(locationRefnum);
            Element qualifierGid = doc.createElement("LocationRefnumQualifierGid");
            locationRefnum.appendChild(qualifierGid);
            Element gid = doc.createElement("Gid");
            qualifierGid.appendChild(gid);
            Element xid = doc.createElement("Xid");
            xid.appendChild(doc.createTextNode(xidName + (i + 1)));
            gid.appendChild(xid);

            Element refnumValue = doc.createElement("LocationRefnumValue");
            refnumValue.appendChild(doc.createTextNode(list.get(i)));
            locationRefnum.appendChild(refnumValue);
        }
    }
}
