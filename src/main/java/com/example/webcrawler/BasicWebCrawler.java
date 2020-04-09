package com.example.webcrawler;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.systemDefault;

public class BasicWebCrawler {

    static class HoldingNode{
        String holdingName;
        String holdingHref;
        String revision;

        public String getHoldingName() {
            return holdingName;
        }

        public String getHoldingHref() {
            return holdingHref;
        }

        public String getRevision() {
            return revision;
        }
    }

    private List<HoldingNode> getUnholdingPlansforYear(String year) {
        String annualProgram = "Annual Program";
        String monthlyProgram = "Monthly Program";
        String holdingName = "Final LNG Unloading Plan";
        Document doc;
        List<HoldingNode> yearNodesList = new ArrayList<>();
        try {
            doc = Jsoup.connect("https://www.desfa.gr/en/regulated-services/lng/users-information-lng" +
                    "/cargoes-unloading-program").get();
            int h2length = doc.select("h2").size();
            for (int i = 0; i <= h2length - 1; i++) {
                Element elem = doc.select("h2").get(i);
                if (elem.text().equals(annualProgram)) {
                    List<Node> annualProgramChildNodes = elem.parentNode().childNodes();
                    for (Node node : annualProgramChildNodes) {
                        Element annualProgramChildNode = (Element) node;
                        if (annualProgramChildNode.select("span").text().equals(year)) {
                            for (Node yearChildNode : annualProgramChildNode.childNodes()) {
                                if (yearChildNode instanceof Element) {
                                    Element annualProgramYearChildNode = (Element) yearChildNode;
                                    if(annualProgramYearChildNode.select("a").text().contains(holdingName)){
                                        HoldingNode holdingNode = new HoldingNode();
                                        holdingNode.holdingName = annualProgramYearChildNode.select("a").text();
                                        holdingNode.revision = holdingNode.holdingName.split("–[^0-9]").length >2 ? holdingNode.holdingName.split("–[^0-9]")[2] : "Revision 0";
                                        holdingNode.holdingHref = annualProgramYearChildNode.select("a").first().attr("abs:href");
                                        yearNodesList.add(holdingNode);
                                    }


                                }
                            }
                        }
                        if (annualProgramChildNode.select("h2").text().equals(monthlyProgram)) {
                            break;
                        }
                    }

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return yearNodesList;
    }

    private void retrieveLatestRevison(String date, String year){
        List<HoldingNode> yearNodesForAnnualProgramList = getUnholdingPlansforYear(year);
        yearNodesForAnnualProgramList.sort(Comparator.comparing(HoldingNode::getRevision).reversed());
        String urlString = yearNodesForAnnualProgramList.get(0).getHoldingHref().replace(" ","%20");
        String fileName = yearNodesForAnnualProgramList.get(0).getHoldingName() + ".xlsx";
        try (BufferedInputStream in = new BufferedInputStream(new URL(urlString).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.err.println("Unable to download the holding file");
        }
        fetchDataForCriteria(fileName,date, year);
    }

    private void fetchDataForCriteria(String fileName, String date, String year){
        double LNG_Cargo_Quantity_m3_LNG;
        double LNG_Cargo_Quantity_KWh;
        LocalDate localdate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        try
        {
            FileInputStream file = new FileInputStream(new File(fileName));

            //Create Workbook instance holding reference to .xlsx file
            XSSFWorkbook workbook = new XSSFWorkbook(file);

            //Get first/desired sheet from the workbook
            XSSFSheet sheet = workbook.getSheetAt(0);

            //Iterate through each rows one by one
            Iterator<Row> rowIterator = sheet.rowIterator();
            while (rowIterator.hasNext())
            {
                XSSFRow row = (XSSFRow) rowIterator.next();
                Cell dateCell = row.getCell(0);


                if(dateCell.getCellType() == Cell.CELL_TYPE_NUMERIC && ofEpochMilli(dateCell.getDateCellValue().getTime()).atZone(systemDefault()).toLocalDate().equals(localdate)){
                    LNG_Cargo_Quantity_m3_LNG = row.getCell(4).getNumericCellValue();
                    LNG_Cargo_Quantity_KWh = row.getCell(5).getNumericCellValue();
                    System.out.println("LNG_Cargo_Quantity_m3_LNG : " + String.format ("%.0f", LNG_Cargo_Quantity_m3_LNG));
                    System.out.println("LNG_Cargo_Quantity_KWh : " + String.format ("%.0f", LNG_Cargo_Quantity_KWh));

                    break;
                }
            }
            System.out.println("Finished Processing the Annual Program for the year : " + year + " and retrieving the quantity for " + date);
            file.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String date = "07-03-2020";
        String year = "2020";
        System.out.println("Processing the Annual Program for the year : " + year + " and retrieving the quantity for " + date);
        BasicWebCrawler crawler = new BasicWebCrawler();
        crawler.retrieveLatestRevison(date, year);

    }


}
