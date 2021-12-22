package metro;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import com.google.gson.*;

public class Main {
    public static void main(String[] args) {
//        String fileName = args[0];
        String fileName = "london.json";

        if (!ReadText.isExist(fileName)) {
            System.out.println("Error! Such a file doesn't exist!");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        Gson gson = new Gson();
        String json = ReadText.readAll(fileName);
        Map<String, Map> map = new HashMap<String, Map>();
        map = (Map<String, Map>)gson.fromJson(json, map.getClass());
        List<MetroLine> metroList = new ArrayList<>();
        for (String name: map.keySet()) {
            metroList.add(new MetroLine(name, (List<Object>)map.get(name)));
        }

        while (true) {
            String input = scanner.nextLine();
            String[] strs = inputParse(input);
            String command = strs[0];
            String lineName = strs[1];
            String stationName = strs[2];
            String lineName2 = strs[3];
            String stationName2 = strs[4];
            double time = 0;
            if ("/exit".equals(command)) {
                break;
            }
            if ("/lines".equals(command)) {
                MetroLine.printLines();
                continue;
            }
            if ("/code".equals(command)) {
                MetroLine.printCode();
                continue;
            }
            MetroLine metroLine = MetroLine.getLine(lineName);
            if (metroLine == null) {
                System.out.println("Invalid command");
                continue;
            }
            MetroLine metroLine2 = null;
            if (!"".equals(lineName2)) {
                metroLine2 = MetroLine.getLine(lineName2);
                if (metroLine2 == null) {
                    System.out.println("Invalid command");
                    continue;
                }    
            }
            if (!"".equals(strs[5])) {
                time = Double.parseDouble(strs[5]);
            }
            switch (command) {
                case "/append":
                case "/add":
                case "add":
                    metroLine.append(stationName, time);
                    break;
                case "/add-head":
                    metroLine.addHead(stationName, time);
                    break;
                case "/remove":
                    metroLine.remove(stationName);
                    break;
                case "/output":
                    metroLine.output(0);
                    break;
                case "/output2":
                    metroLine.output(1);
                    break;
                case "/output3":
                    metroLine.output(2);
                    break;
                case "/connect":
                    if (metroLine2 == null) {
                        System.out.println("Invalid command");
                        break;
                    }    
                    metroLine.connect(stationName, metroLine2, stationName2);
                    break;
                case "/route":
                    if (metroLine2 == null) {
                        System.out.println("Invalid command");
                        break;
                    }    
                    metroLine.route(stationName, metroLine2, stationName2);
                    break;
                case "/fastest-route":
                    if (metroLine2 == null) {
                        System.out.println("Invalid command");
                        break;
                    }    
                    metroLine.fastestRoute(stationName, metroLine2, stationName2);
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }    
        scanner.close();
    }

    static String[] inputParse(String input) {
        String[] result = new String[6];
        String command = "";
        String lineName = "";
        String stationName = "";
        String lineName2 = "";
        String stationName2 = "";
        String time = "";
        Pattern p = Pattern.compile("(\"([^\"]*)\"|([/\\w][\\w-]*)|[0-9.]+)");
        Matcher m = p.matcher(input);
        if (m.find()) {
            command = stripDoubleQuote(m.group(1));
        }
        if (m.find()) {
            lineName = stripDoubleQuote(m.group(1));      // must group(1)
        }
        if (m.find()) {
            stationName = stripDoubleQuote(m.group(1));       // must group(1)
        }
        if (m.find()) {
            lineName2 = stripDoubleQuote(m.group(1));      // must group(1)
        }
        if (m.find()) {
            stationName2 = stripDoubleQuote(m.group(1));       // must group(1)
        }
        if (lineName2.matches("[0-9.]+")) {
            time = lineName2;
            lineName2 = "";
        }
        result[0] = command;
        result[1] = lineName;
        result[2] = stationName;
        result[3] = lineName2;
        result[4] = stationName2;
        result[5] = time;
        return result;
    }

    static String stripDoubleQuote(String str) {
        if(str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"') {
          return str.substring(1, str.length() - 1);
        }else {
          return str;
        }
      }
}

class MetroLine {

    static List<MetroLine> lineList = new ArrayList<>();
    String name;
    List<Station> stations;
    int id;
    Station firstStation = null;
    Station lastStation = null;

    MetroLine(String name, List<Object> list) {
        this.name = name;
        stations = new ArrayList<>();
        for (Object obj: list) {
            Map<String, Object> obj2 = (Map<String, Object>)obj;
            String stationName = (String) obj2.get("name");
            List<String> prev = (List<String>)obj2.get("prev");
            List<String> next = (List<String>)obj2.get("next");
            List<Object> transfer = (List<Object>)obj2.get("transfer");
            Double time = (Double)obj2.get("time");
            Station station = new Station(this, stationName, prev, next, transfer, time);
            stations.add(station);
            if (prev.size() == 0) {
                firstStation = station;
            }
            if (next.size() == 0) {
                lastStation = station;
            }
        } 
        lineList.add(this);
    }

    void append(String stationName, double time) {
        if (lastStation == null) {
            return;
        }
        lastStation.next.add(stationName);
        lastStation = new Station(this, stationName, lastStation.getName(), null, time);
        stations.add(lastStation);
    }
 
    void addHead(String stationName, double time) {
        if (firstStation == null) {
            return;
        }
        firstStation.prev.add(stationName);
        firstStation = new Station(this, stationName, null, firstStation.getName(), time);
        stations.add(0, firstStation);
    }

    void remove(String stationName) {
        stations.remove(getStation(stationName));
    }

    void output(int option) {
        if (stations.size() == 0) {
            return;
        }
        for (Station station: stations) {
            station.outputDone = false;
        }
        System.out.println("depot");
        Station station = firstStation;
        for (;station != null && !station.outputDone;) {
            station.outputDone = true;
            String stationName = station.getName();
            List<Transfer> transferList = station.getTransferList(); 
            if (transferList.size() == 0) {
                System.out.println(stationName);
            } else {
                System.out.print(stationName);
                for (Transfer transfer: transferList) {
                    System.out.println(" - " + transfer.getStationName() + " (" + transfer.getLineName() + ")");
                }
            }
            if (option == 0) {
                station = getNext1st(station);
            } else if (option == 1) {
                station = getNext2nd(station);
            } else if (option == 2) {
                station = getNext3rd(station);
            }
        }
        System.out.println("depot");
    }

    Station getNext1st(Station station) {
        List<Station> nextStations = getNext(station);
        if (nextStations.size() == 0) {
            return null;
        } else {
            return nextStations.get(0);
        }
    }

    Station getNext2nd(Station station) {
        List<Station> nextStations = getNext(station);
        if (nextStations.size() == 0) {
            return null;
        } else if (nextStations.size() == 1) {
            return nextStations.get(0);
        } else {
            return nextStations.get(1); 
        }
    }

    Station getNext3rd(Station station) {
        List<Station> nextStations = getNext(station);
        if (nextStations.size() == 0) {
            return null;
        } else if (nextStations.size() == 1) {
            return nextStations.get(0);
        } else if (nextStations.size() == 2) {
            return nextStations.get(1);
        } else {
            return nextStations.get(2); 
        }
    }

    List<Station> getNext(Station station) {
        List<Station> nextStations = new ArrayList<>();
        for (String stationName: station.next) {
            nextStations.add(station.line.getStation(stationName));
        } 
        return nextStations;
    }

    List<Station> getNext(int id) {
        return getNext(getStation(id));
    }

    List<Station> getPrev(Station station) {
        List<Station> prevStations = new ArrayList<>();
        for (String stationName: station.prev) {
            prevStations.add(station.line.getStation(stationName));
        } 
        return prevStations;
    }

    List<Station> getPrev(int id) {
        return getPrev(getStation(id));
    }

    void connect(String stationName, MetroLine otherMetroLine, String otherStationName) {
        addTransfer(stationName, otherMetroLine.getName(), otherStationName);
        otherMetroLine.addTransfer(otherStationName, name, stationName);
    }

    void route(String stationName, MetroLine targetMetroLine, String targetStationName) {
        List<Integer> routeList = routeByBWS(stationName, targetMetroLine, targetStationName);
        int backId = routeList.get(0);
        for (int id: routeList) {
            if (id / 100 != backId / 100) {
                System.out.println("Transition to line " + getLine(id / 100).getName());
            }
            System.out.println(getStation(id).getName());
            backId = id;
        }
    }

    void fastestRoute(String stationName, MetroLine targetMetroLine, String targetStationName) {
        List<Integer> routeList = routeByDijkstra(stationName, targetMetroLine, targetStationName);
        int backId = routeList.get(0);
        for (int id: routeList) {
            if (id / 100 != backId / 100) {
                System.out.println("Transition to line " + getLine(id / 100).getName());
            }
            System.out.println(getStation(id).getName());
            backId = id;
        }
        Station targetStation = getStation(routeList.get(routeList.size() - 1));
        System.out.println("Total: " + targetStation.ddistance + " minutes in the way");
    }

    void addTransfer(String stationName, String otherLineName, String otherStationName) {
        Station station = getStation(stationName);
        station.transferList.add(new Transfer(otherLineName, otherStationName));
    }

    String getName() {
        return name;
    }

    Station getStation(String stationName) {
        for (Station station: stations) {
            if (stationName.equals(station.getName())) {
                return station;
            }
        }
        return null;
    }     

    Station getStation(int id) {
        MetroLine metroLine = getLine(id / 100);
        for (Station station: metroLine.stations) {
            if (station.id == id) {
                return station;
            }
        }
        return null;
    }

    Station getStation(Transfer transfer) {
        return getStation(transfer.lineName, transfer.stationName);
    }

    Station getStation(String lineName, String stationName) {
        MetroLine line = MetroLine.getLine(lineName);
        return line.getStation(stationName);
    }

    List<Integer> routeByBWS(String stationName, MetroLine targetMetroLine, String targetStationName) {
        prepRoute();
        Station startStation = getStation(stationName);
        Station targetStation = targetMetroLine.getStation(targetStationName);
        startStation.distance = 0;
        List<Station> markStations = new ArrayList<>();
        markStations.add(startStation);
        for (int d = 1;; d++) {
            List<Station> nextMarkStations = new ArrayList<>();
            for (Station markStation: markStations) {
                List<Station> neighbourStations = getNeighbourStations(markStation);
                for (Station station: neighbourStations) {
                    station.distance = d;
                    if (station.id == targetStation.id) {
                        return buildRoute(station);
                    }
                }
                nextMarkStations.addAll(neighbourStations);
            }
            markStations = nextMarkStations;
        }
    }

    List<Integer> buildRoute(Station targetStation) {
        List<Integer> route = new ArrayList<>();
        route.add(targetStation.id);
        Station station = targetStation;
        while (station.distance > 0) {
            station = getStation(station.backId);
            route.add(station.id);
        }
        Collections.reverse(route);
        return route;
    }

    List<Station> getNeighbourStations(Station station) {
        List<Station> neighbourStations = new ArrayList<>();
        for (Transfer transfer: station.transferList) {
            Station transferStation = getStation(transfer);
            if (transferStation.distance == -1) {
                transferStation.backId = station.id;
                neighbourStations.add(transferStation);
            }       
        }    
        List<Station> backStations = getPrev(station.id);
        List<Station> nextStations = getNext(station.id);
        for (Station backStation: backStations) {
            if (backStation != null && backStation.distance == -1) {
                backStation.backId = station.id;
                neighbourStations.add(backStation);
                for (Transfer transfer: backStation.transferList) {
                    Station transferStation = getStation(transfer);
                    if (transferStation.distance == -1) {
                        transferStation.backId = backStation.id;
                        neighbourStations.add(transferStation);
                    }       
                }    
            }
        }
        for (Station nextStation: nextStations) {
            if (nextStation != null && nextStation.distance == -1) {
                nextStation.backId = station.id;
                neighbourStations.add(nextStation);
                for (Transfer transfer: nextStation.transferList) {
                    Station transferStation = getStation(transfer);
                    if (transferStation.distance == -1) {
                        transferStation.backId = nextStation.id;
                        neighbourStations.add(transferStation);
                    }       
                }    
            }
        }
        return neighbourStations;
    }

    void prepRoute() {
        int lineId = 0; 
        for (MetroLine metroLine: lineList) {
            lineId ++;
            metroLine.id = lineId;
            int stationId = 0;
            for (Station station: metroLine.stations) {
                stationId ++;
                station.id  = lineId * 100 + stationId;
                station.distance = -1;
            }
        }
    }

    MetroLine getLine(int id) {
        for (MetroLine metroLine: lineList) {
            if (id == metroLine.id) {
                return metroLine;
            }
        }
        return null;
    }

    static MetroLine getLine(String lineName) {
        for (MetroLine metroLine: lineList) {
            if (lineName.equals(metroLine.name)) {
                return metroLine;
            }
        }
        return null;
    }

    List<Integer> routeByDijkstra(String stationName, MetroLine targetMetroLine, String targetStationName) {
        prepRouteDijkstra();
        Station startStation = getStation(stationName);
        Station targetStation = targetMetroLine.getStation(targetStationName);
        startStation.ddistance = 0;
        for (;;) {
            Station pibotStation = getUnMarkedMinDStation();
            if (pibotStation == null) {
                break;
            }
            pibotStation.marked = true;
            if (pibotStation.id == targetStation.id) {
                return buildRouteDijkstra(pibotStation);
            }
            updateNeighbourStationsDijkstra(pibotStation);
        }
        return null;
    }

    List<Integer> buildRouteDijkstra(Station targetStation) {
        List<Integer> route = new ArrayList<>();
        route.add(targetStation.id);
        Station station = targetStation;
        while (station.ddistance > 0) {
            station = getStation(station.backId);
            route.add(station.id);
        }
        Collections.reverse(route);
        return route;
    }

    void prepRouteDijkstra() {
        int lineId = 0; 
        for (MetroLine metroLine: lineList) {
            lineId ++;
            metroLine.id = lineId;
            int stationId = 0;
            for (Station station: metroLine.stations) {
                stationId ++;
                station.id  = lineId * 100 + stationId;
                station.ddistance = Double.MAX_VALUE;
                station.marked = false;
            }
        }
    }

    Station getUnMarkedMinDStation() {
        double mindistance = Double.MAX_VALUE;
        Station minStation = null;
        for (MetroLine metroLine: lineList) {
            for (Station station: metroLine.stations) {
                if (!station.marked) {
                    if (station.ddistance < mindistance) {
                        minStation = station;
                        mindistance = station.ddistance;
                    }
                }
            }
        }
        return minStation;
    }

    void updateNeighbourStationsDijkstra(Station station) {
        List<Station> backStations = getPrev(station.id);
        List<Station> nextStations = getNext(station.id);
        for (Station backStation: backStations) {
            if (backStation != null && !backStation.marked) {
                if (station.ddistance + backStation.time < backStation.ddistance) {
                    backStation.ddistance = station.ddistance + backStation.time;
                    backStation.backId = station.id;
                }
            }
        }
        for (Station nextStation: nextStations) {
            if (nextStation != null && !nextStation.marked) {
                if (station.ddistance + station.time < nextStation.ddistance) {
                    nextStation.ddistance = station.ddistance + station.time;
                    nextStation.backId = station.id;
                }
            }
        }
        for (Transfer transfer: station.transferList) {
            Station transferStation = getStation(transfer);
            if (!transferStation.marked) {
                if (station.ddistance + 5.0 < transferStation.ddistance) {
                    transferStation.ddistance = station.ddistance + 5.0;                   
                    transferStation.backId = station.id;
                }
            }       
        }    
    }

    static void printLines() {
        for (MetroLine line: lineList) {
            System.out.println(line.getName());
        }
    }

    static void printCode() {
        lineList.get(0).prepRouteDijkstra();
        for (MetroLine line: lineList) {
            System.out.println("" + line.id + " " + line.getName());
            for (Station station: line.stations) {
                String str = "  " + station.id + " " + station.getName();
                List<String> listP = new ArrayList<>();
                for (String prevName: station.prev) {
                    Station prevStation = line.getStation(prevName);
                    listP.add("" + prevStation.id);
                }
                List<String> listN = new ArrayList<>();
                for (String nextName: station.next) {
                    Station nextStation = line.getStation(nextName);
                    listN.add("" + nextStation.id);
                }
                List<String> listT = new ArrayList<>();
                for (Transfer transfer: station.transferList) {
                    Station transferStation = line.getStation(transfer);
                    listT.add("" + transferStation.id);
                }
                if (listP.size() > 0) {
                    str += " Prev " + String.join(" ", listP);
                }
                if (listN.size() > 0) {
                    str += " Next " + String.join(" ", listN);
                }
                if (listT.size() > 0) {
                    str += " Transfer " + String.join(" ", listT);
                }
                System.out.println(str);
            }
        }
    }
}

class Station {

    MetroLine line;
    String name;
    List<String> prev;
    List<String> next;
    List<Transfer> transferList;
    double time;
    int distance = -1;
    int id;
    int backId;
    boolean marked;
    double ddistance;
    boolean outputDone;

    Station(MetroLine line, String name, List<String> prev, List<String> next, List<Object> transfer, Double time) {
        this.line = line;
        this.name = name;
        this.prev = prev;
        this.next = next;
        if (time != null) {
            this.time = time;
        } else {
            this.time = 0;
        }
        transferList = new ArrayList<>(); 
        for (Object obj: transfer) {
            Map<String, String> map = (Map<String, String>) obj;
            transferList.add(new Transfer((String)map.get("line"), (String)map.get("station")));
        }
    }

    Station(MetroLine line, String name, String prevStationName, String nextStationName, double time) {
        this.line = line;
        this.name = name;
        prev = new ArrayList<>();
        if (prevStationName != null) {
            prev.add(prevStationName);
        }
        next = new ArrayList<>();
        if (nextStationName != null) {
            next.add(nextStationName);
        }
        transferList = new ArrayList<>(); 
        this.time = time;
    }

    Station(MetroLine line, String name, double time) {
        this.line = line;
        this.name = name;
        prev = new ArrayList<>();
        next = new ArrayList<>();
        transferList = new ArrayList<>(); 
        this.time = time;
    }

    Station(String name) {
        this.name = name;
        prev = new ArrayList<>();
        next = new ArrayList<>();
        transferList = new ArrayList<>(); 
        this.time = 0;
    }

    String getName() {
        return name;
    }

    List<Transfer> getTransferList() {
        return transferList;
    }
}

class Transfer {

    String lineName;
    String stationName;

    Transfer(String lineName, String stationName) {
        this.lineName = lineName;
        this.stationName = stationName;
    }

    String getStationName() {
        return stationName;
    }

    String getLineName() {
        return lineName;
    }
} 

class ReadText {

    static boolean isExist(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    static String getAbsolutePath(String fileName) {
        File file = new File(fileName);
        return file.getAbsolutePath();
    }

    static String readAllWithoutEol(String fileName) {
        String text = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));   
            text =  br.lines().collect(Collectors.joining());        
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return text;
    }

    static List<String> readLines(String fileName) {
        List<String> lines = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));   
            lines =  br.lines().collect(Collectors.toList());        
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return lines;
    }

    static String readAll(String fileName) {
        char[] cbuf = new char[4096];
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));           
            while (true) {
                int length = br.read(cbuf, 0, cbuf.length);
                if (length != -1) {
                    sb.append(cbuf, 0, length);
                }
                if (length < cbuf.length) {
                    break;
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return sb.toString();
    }

    static String readAll(String fileName, String encoding) {
        char[] cbuf = new char[4096];
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), encoding));
            while (true) {
                int length = br.read(cbuf, 0, cbuf.length);
                if (length != -1) {
                    sb.append(cbuf, 0, length);
                }
                if (length < cbuf.length) {
                    break;
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return sb.toString();
    }
}

class WriteText {

    static boolean writeAll(String fileName, String text) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
            bw.write(text, 0, text.length());
            bw.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    static boolean writeAll(String fileName, String text, String encoding) {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), encoding));
            bw.write(text, 0, text.length());
            bw.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}