import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class A3 {
    public static void main(String[] args) throws IOException {
        String dataset = args[0];
        Graph graph = new Graph();
        // build graph
        handleFiles(new File(dataset), graph);

        // use dfs to get connectors
        List<Node> connectors = graph.getConnectors();
        for (Node connector : connectors) {
            System.out.println(connector.getEmail());
        }
        if (args.length > 1) {
            writeConnectorsToFile(args[1], connectors);
        }
        // build disjoint set
        DisjointSet disjointSet = new DisjointSet(Graph.nodeIdGenerator);
        for (Node node : graph.getAllNodes()) {
            for (Node neighbor : node.getNeighbors()) {
                disjointSet.union(node.getNodeId(), neighbor.getNodeId());
            }
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Email address of the individual (or EXIT to quit): ");
            String input = scanner.nextLine().strip();
            if (input.equalsIgnoreCase("exit")) {
                break;
            }
            Node nodeByEmail = graph.getNodeByEmail(input);
            if (nodeByEmail == null) {
                System.out.printf("Email address (%s) not found in the dataset.%n", input);
                continue;
            }
            System.out.printf("* %s has sent messages to %d others%n",
                    input, nodeByEmail.getSentTo().size());
            System.out.printf("* %s has received messages from %d others%n",
                    input, nodeByEmail.getReceivedFrom().size());
            System.out.printf("* %s is in a team with %d individuals%n",
                    input, disjointSet.numOfNodesInSameSet(nodeByEmail.getNodeId()));
        }
    }

    private static void writeConnectorsToFile(String filename, List<Node> connectors)
            throws IOException {
        FileWriter fileWriter = new FileWriter(filename);
        for (Node node : connectors) {
            fileWriter.write(node.getEmail() + "\n");
        }
        fileWriter.flush();
        fileWriter.close();
    }

    private static void handleFiles(File path, Graph graph) throws IOException {
        if (path.isDirectory()) {
            File[] subFiles = path.listFiles();
            if (subFiles == null) {
                return;
            }
            for (File subfile : subFiles) {
                handleFiles(subfile, graph);
            }
        } else {
            Scanner scanner = new Scanner(new FileInputStream(path));
            String fromEmail = null;
            List<String> toEmails = new ArrayList<>();
            int cnt = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                cnt++;
                if (cnt == 3 && line.startsWith("From")) {
                    String[] items = line.strip().split(":");
                    if (items.length < 2) { // invalid email file
                        return;
                    }
                    fromEmail = items[1].strip();
                } else if (cnt == 4 && line.startsWith("To")) {
                    line = line.substring(3).strip();
                    do {
                        String[] items = line.strip().split(",");
                        for (String item : items) {
                            if (item.strip().length() == 0) {
                                continue;
                            }
                            toEmails.add(item.strip());
                        }
                        if (scanner.hasNextLine()) {
                            line = scanner.nextLine();
                        }
                        if (line.startsWith("Subject")) {
                            break;
                        }
                    } while (scanner.hasNextLine());
                    break;
                }
            }
            scanner.close();
            if (fromEmail == null || toEmails.isEmpty()) {
                return;
            }
            for (String toEmail : toEmails) {
                graph.addEdge(fromEmail, toEmail);
            }
        }
    }
}


class Graph {
    public static int nodeIdGenerator = 1;

    // Mapping from email to graph nodes
    private Map<String, Node> nodes;

    // mapping from node id to nodes
    private Map<Integer, Node> idMap;

    public Graph() {
        nodes = new HashMap<>();
        idMap = new HashMap<>();
    }

    public Node getNodeByEmail(String email) {
        return nodes.getOrDefault(email, null);
    }

    public void addEdge(String fromEmail, String toEmail) {
        if (!nodes.containsKey(fromEmail)) {
            Node node = new Node(fromEmail, nodeIdGenerator++);
            nodes.put(fromEmail, node);
            idMap.put(node.getNodeId(), node);
        }
        if (!nodes.containsKey(toEmail)) {
            Node node = new Node(fromEmail, nodeIdGenerator++);
            nodes.put(toEmail, new Node(toEmail, nodeIdGenerator++));
            idMap.put(node.getNodeId(), node);
        }
        if (fromEmail.equals(toEmail)) {
            return;
        }
        Node fromNode = nodes.get(fromEmail);
        Node toNode = nodes.get(toEmail);
        fromNode.addNeighbor(toNode);
        toNode.addNeighbor(fromNode);

        fromNode.addSentTo(toNode);
        toNode.addReceivedFrom(fromNode);
    }

    public List<Node> getConnectors() {
        List<Node> connectors = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (!node.isVisited()) {
                dfs(node, 1, connectors);
            }
        }
        return connectors;
    }

    public void dfs(Node node, int depth, List<Node> connectors) {
        node.setVisited(true);
        node.setDfsNum(depth);
        node.setBack(depth);
        int childNumber = 0;
        boolean isConnector = false;

        for (Node neighbor : node.getNeighbors()) {
            if (!neighbor.isVisited()) {
                neighbor.setParent(node);
                dfs(neighbor, depth + 1, connectors);
                childNumber++;
                if (neighbor.getBack() >= node.getDfsNum()) {
                    isConnector = true;
                }
                node.setBack(Math.min(node.getBack(), neighbor.getBack()));
            } else if (!node.equals(neighbor.getParent())) {
                node.setBack(Math.min(node.getBack(), neighbor.getDfsNum()));
            }
        }
        if (node.getParent() != null && isConnector
                || node.getParent() == null && childNumber > 1) {
            connectors.add(node);
        }
    }

    public Collection<Node> getAllNodes() {
        return nodes.values();
    }
}

class Node {
    private int nodeId;
    private String email;
    // Mapping from email to Node
    private Map<String, Node> neighbors;
    // dfs number, assigned when a vertex is visited,
    // dealt out in increasing order.
    private int dfsNum;
    // initially assigned when a vertex is visited, and is equal to dfs num,
    // but can be changed later as follows:
    private int back;
    // number of messages sent to others
    private Set<Node> sentTo;
    // number of messages received from others
    private Set<Node> receivedFrom;
    // used to identify connectors
    private Node parent;

    private boolean visited;

    public Node(String email, int nodeId) {
        this.nodeId = nodeId;
        this.email = email;
        neighbors = new HashMap<>();
        sentTo = new HashSet<>();
        receivedFrom = new HashSet<>();
    }

    public String getEmail() {
        return email;
    }

    /**
     * Add a neighbor of this node.
     *
     * @param node
     */
    public void addNeighbor(Node node) {
        if (neighbors.containsKey(node.getEmail())) {
            return;
        }
        neighbors.put(node.getEmail(), node);
    }

    /**
     * Return neighbors of this node.
     *
     * @return neighbors of this node.
     */
    public Collection<Node> getNeighbors() {
        return neighbors.values();
    }

    public int getDfsNum() {
        return dfsNum;
    }

    public int getBack() {
        return back;
    }

    public void setBack(int back) {
        this.back = back;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(email, node.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    public Set<Node> getSentTo() {
        return sentTo;
    }

    public Set<Node> getReceivedFrom() {
        return receivedFrom;
    }

    public void addReceivedFrom(Node fromNode) {
        receivedFrom.add(fromNode);
    }

    public void addSentTo(Node toNode) {
        sentTo.add(toNode);
    }

    public int getNodeId() {
        return nodeId;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public void setDfsNum(int dfsNum) {
        this.dfsNum = dfsNum;
    }
}

class DisjointSet {
    int[] parent;
    int[] rank;
    int[] count;
    int max = 0;

    public DisjointSet(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        rank = new int[n];
        count = new int[n];
        Arrays.fill(count, 1);
    }

    public void reset() {
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
        Arrays.fill(count, 1);
        max = 0;
    }

    // Returns the root of the specified collection
    public int find(int index) {
        if (parent[index] != index) {
            parent[index] = find(parent[index]);
        }
        return parent[index];
    }

    // Merge the two collections
    public void union(int x, int y) {
        int xp = find(x);
        int yp = find(y);
        // x & y already in the same set
        if (xp == yp) return;

        if (rank[xp] < rank[yp]) {
            parent[xp] = yp;
            count[xp] += count[yp];
            max = Math.max(count[xp], max);
        } else if (rank[xp] > rank[yp]) {
            parent[yp] = xp;
            count[yp] += count[xp];
            max = Math.max(count[yp], max);
        } else {
            parent[xp] = yp;
            count[xp] += count[yp];
            max = Math.max(count[xp], max);
            rank[yp]++;
        }
    }

    public int numOfNodesInSameSet(int nodeId) {
        int cnt = 0;
        int root = find(nodeId);
        for (int i = 1; i < parent.length; i++) {
            if (i == nodeId) {
                continue;
            }
            if (find(i) == root) {
                cnt++;
            }
        }
        return cnt;
    }
}



