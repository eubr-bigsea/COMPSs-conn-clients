package es.bsc.conn.clients.slurm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.conn.clients.loggers.Loggers;


public class JobDescription {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.SLURM);
    private static final String TRES = "TRES";
    public static final String NUM_NODES = "NumNodes";
    public static final String NUM_CPUS = "NumCPUs";
    public static final String CPUS_TASK = "CPUs/Task";
    public static final String MEM = "mem";
    public static final String GRES = "Gres";
    private static final String NULL = "(null)";

    private Map<String, String> jobProperties;
    private List<String> nodeList = new LinkedList<>();


    /**
     * New job description with the given properties that are parsed
     * 
     * @param description
     */
    public JobDescription(String description) {
        String[] pars = description.split(" ");
        if (pars != null && pars.length > 0) {
            jobProperties = new HashMap<String, String>(pars.length);
            for (String par : pars) {
                int eqIndex = par.indexOf('=');
                if (eqIndex >= 0) {
                    String key = par.substring(0, eqIndex);
                    String value = par.substring(eqIndex + 1);
                    if (key.equals(TRES)) {
                        parseTREC(value);
                    } else if ("NodeList".equals(key)) {
                        LOGGER.debug("Parsing node list: " + value);
                        parseNodelist(value, nodeList);
                    } else {
                        if (value.equals(NULL)) {
                            value = "";
                        }
                        jobProperties.put(key, value);
                    }
                } else {
                    // LOGGER.debug("parameter " + par + " is not following the key=value. Skipping...");
                }
            }
        }
    }

    /**
     * New job description with the given properties
     * 
     * @param jobProp
     */
    public JobDescription(Map<String, String> jobProp) {
        jobProperties = jobProp;
    }

    public static void parseNodelist(String value, List<String> nodeList2) {
        int startPosition = 0;
        int nextStBrack = value.indexOf('[');
        int nextEndBrack = value.indexOf(']');
        int nextComma = value.indexOf(',');
        String nodeGroup = value;
        while (nextComma > 0) {
            if (nextStBrack > 0 && nextEndBrack > 0 && nextComma > nextStBrack) {
                // Comma between the brackets
                nodeGroup = value.substring(startPosition, nextEndBrack + 1);
                startPosition = nextEndBrack + 2;
            } else {
                // Comma before or after brackets or not brackets
                nodeGroup = value.substring(startPosition, nextComma);
                startPosition = nextComma + 1;
            }
            // parsing a node group
            if (nodeGroup != null && !nodeGroup.isEmpty()) {
                if (nodeGroup.contains("[")) {
                    manageNodeGroup(nodeGroup, nodeList2);
                } else {
                    nodeList2.add(nodeGroup);
                }
            }
            nextStBrack = value.indexOf(startPosition, '[');
            nextEndBrack = value.indexOf(startPosition, ']');
            nextComma = value.indexOf(startPosition, ',');
        }
        
        // Check if there is another node to group at the end
        if (startPosition >= 0 && startPosition < value.length()) {
            nodeGroup = value.substring(startPosition);
            if (nodeGroup.contains("[")) {
                manageNodeGroup(nodeGroup, nodeList2);
            } else {
                nodeList2.add(nodeGroup);
            }
        }

    }

    private static void manageNodeGroup(String nodeGroup, List<String> nodeList2) {
        String nodeRoot = nodeGroup.substring(0, nodeGroup.indexOf('['));
        String numliststr = nodeGroup.substring(nodeGroup.indexOf('[') + 1, nodeGroup.indexOf(']'));
        if (numliststr.contains(",")) {
            String[] numList = numliststr.split(",");
            for (String num : numList) {
                if (num.contains("-")) {
                    addNodesInInterval(num, nodeRoot, nodeList2);
                } else {
                    nodeList2.add(nodeRoot + num);
                }
            }
        } else if (numliststr.contains("-")) {
            addNodesInInterval(numliststr, nodeRoot, nodeList2);
        }

    }

    private static void addNodesInInterval(String numInterval, String nodeRoot, List<String> nodeList2) {
        // Treat number intervals A-B
        int init = Integer.parseInt(numInterval.substring(0, numInterval.indexOf('-')));
        int end = Integer.parseInt(numInterval.substring(numInterval.indexOf('-') + 1));
        for (int i = init; i <= end; i++) {
            nodeList2.add(nodeRoot + i);
        }

    }

    private void parseTREC(String value) {
        String[] trecs = value.split(",");
        if (trecs != null && trecs.length > 0) {
            for (String trec : trecs) {
                int eqIndex = trec.indexOf('=');
                jobProperties.put(trec.substring(0, eqIndex), trec.substring(eqIndex + 1));
            }
        }

    }

    public String generateRequest() {
        StringBuilder sb = new StringBuilder();
        String nodes = jobProperties.get(NUM_NODES);
        if (nodes == null || nodes.isEmpty()) {
            LOGGER.warn("Num nodes for SLURM is not defined. Seting one by default");
            nodes = "1";
        }
        sb.append("-N" + nodes);
        String cpusStr = jobProperties.get(NUM_CPUS);
        String cpusTaskStr = jobProperties.get(CPUS_TASK);
        int cpusTask, cpus, tasks;
        if (cpusTaskStr == null || cpusTaskStr.isEmpty()) {
            cpusTask = 1;
        } else {
            cpusTask = Integer.parseInt(cpusTaskStr);
            sb.append(" --cpus_per_task=" + cpusTask);
        }
        if (cpusStr == null || cpusStr.isEmpty()) {
            cpus = cpusTask;
        } else {
            cpus = Integer.parseInt(cpusStr);
        }
        tasks = cpus / cpusTask;
        sb.append(" -n" + tasks);
        String memStr = jobProperties.get(MEM);
        if (memStr != null && !memStr.isEmpty()) {
            sb.append(" --mem=" + memStr);
        }
        String gresStr = jobProperties.get(GRES);
        if (gresStr != null && !gresStr.isEmpty()) {
            sb.append(" --gres=" + gresStr);
        }
        return sb.toString();
    }

    public List<String> getNodeList() {
        return nodeList;
    }

    public String getProperty(String string) {
        return jobProperties.get(string);
    }

}
