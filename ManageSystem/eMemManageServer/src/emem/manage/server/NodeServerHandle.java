package emem.manage.server;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Scanner;

import emem.manage.model.NodeModel;
import emem.manage.parser.NodeParser;
import emem.manage.parser.Parser;

public class NodeServerHandle implements Runnable {

	private SocketChannel channel;
	private Map<String, Object> result;
	private NodeModel model;

	public NodeServerHandle(SocketChannel channel) {
		this.channel = channel;
		try {
			this.model = new NodeModel(getHost(channel.getRemoteAddress()
					.toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.model = new NodeModel();
			e.printStackTrace();
		}
	}

	public void run() {
		doParse();
		doMongo();
	}

	private void doParse() {
		Scanner scanner = new Scanner(channel);
		Parser p = new NodeParser();
		p.parse(scanner);
		result = p.getResult();
	}

	private void doMongo() {
//		for (Map.Entry<String, Object> entry : result.entrySet()) {
//			System.out.println(entry.getKey() + "---->" + entry.getValue());
//		}
		
		model.infoInsert(result);
		model.update(result);
	}

	private String getHost(String address) {
		return address.replace("/", "").split(":")[0];
	}

}
