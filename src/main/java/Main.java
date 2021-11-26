import babel.Babel;
import babel.exceptions.InvalidParameterException;
import babel.generic.GenericProtocol;
import channels.MultiLoggerChannelInitializer;
import network.data.Host;
import protocols.app.Application;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import protocols.dissemination.flood.Flood;
import protocols.membership.cyclon.Cyclon;
import protocols.membership.cyclonresest.CyclonResEst;
import protocols.membership.hyparviewintegrated.HyparViewIntegrated;
import protocols.membership.hyparviewresest.HyparViewResEst;
import protocols.optimization.foutakos.Foutakos;
import protocols.optimization.resest.ResEst;
import utils.BabelConfigValidator;
import utils.ExistsFileValidor;
import utils.InterfaceToIp;
import utils.Translate;
import protocols.membership.hyparview.HyparView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class Main {

    public static class Args {

        @Parameter(names = {"--help", "-h"}, help = true)
        public boolean help;


        @Parameter(names = "-membership")
        public String membershipProtocol = "hyparview";
        @Parameter(names = "-service")
        public String service = "flood";

        @Parameter(names = "-bootstraps") //v
        public String bootstraps = "";
        @Parameter(names = "-listenIP") //v
        public String listenIP = "";
        @Parameter(names = "-listenPort") //v
        public int listenPort = 10000;
        @Parameter(names = "-rport") //v
        public boolean randomPort = false;
        @Parameter(names = "-listenInterface") //v
        public String listenInterface = "eth0";
        @Parameter(names = "-hpvRemovalMode") //v
        public String hpvRemovalMode = "random";

        @Parameter(names = "-peerID") //v
        public int peerId = -1;
        @Parameter(names = "-startTime")
        public int startT = -1;
        @Parameter(names = "-resEstConfig")
        public String resEstConfig = "config_1000nodes_hyparview_lognormal_95_015.properties";

        @Parameter(names = "-foutakosConfig")
        public String foutakosConfig = "config_1rot_average.properties";

        @Parameter(names = "-appConfig") //v
        public String appConfig = "config_01s_100bytes.properties";

        @Parameter(names = "-noOptimization")
        public boolean noOptimizationFlag = false;


        @Parameter(names = "-babelConfFile", description = "Babel configuration file", validateWith = ExistsFileValidor.class)
        public String babelConf = DEFAULT_CONF;

        @Parameter(names = "-babelConf", description = "Babel configuration", validateWith = BabelConfigValidator.class)
        public List<String> babelArgs = new ArrayList<>();

    }

    private static final String DEFAULT_CONF = "config/network_config.properties";

    public static final String PROTO_CHANNELS = "SharedTCP";

    static {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
    }

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        Args cli = new Args();
        JCommander jc = JCommander.newBuilder().addObject(cli).build();
        new JCommander();

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            logger.error(e.getMessage());
            e.usage();
            System.exit(1);
        }

        if(cli.help) {
            jc.usage();
            System.exit(1);
        }


        Babel babel = Babel.getInstance();
        babel.registerChannelInitializer(PROTO_CHANNELS, new MultiLoggerChannelInitializer());


        List<String> overlayArgs = new ArrayList<>();
        if(!cli.bootstraps.equals("")) {
            overlayArgs.add("contacts="+cli.bootstraps);
        }
        if(cli.listenIP.equals("")) {
            if(cli.listenInterface.equals("")) {
                logger.error("No IP or interface?");
                throw new Exception("No IP nor interface...");
            } else {
                overlayArgs.add("listenInterface="+cli.listenInterface);
            }
        } else {
            overlayArgs.add("address="+cli.listenIP);
        }
        if(cli.randomPort) {
            overlayArgs.add("port=" + (10000 + new Random().nextInt(1000)));
        } else {
            overlayArgs.add("port="+cli.listenPort);
        }
        overlayArgs.add("hpvRemovalMode="+cli.hpvRemovalMode);
        String[] overlayArgsArr = new String[overlayArgs.size()];
        overlayArgs.toArray(overlayArgsArr);
        Properties overlayProps = babel.loadConfig("babel-config/props.properties", overlayArgsArr);
        addInterfaceIp(overlayProps);
        //overlayProps.setProperty("nThreads", "4"); //TODO remove only if tests not ok
        Host myself =  new Host(InetAddress.getByName(overlayProps.getProperty("address")),
                Integer.parseInt(overlayProps.getProperty("port")));
        logger.info("Hello, I am {}", myself);
        logger.info("Loading overlay {}", cli.membershipProtocol);
        GenericProtocol membershipProtocol = null;
        switch (cli.membershipProtocol) {
            case "hyparview":
                Translate.addId(HyparView.PROTOCOL_ID, HyparView.PROTOCOL_NAME);
                membershipProtocol = new HyparView(PROTO_CHANNELS, overlayProps, myself);
                babel.registerProtocol(membershipProtocol);
                break;
            case "cyclon":
                Translate.addId(Cyclon.PROTOCOL_ID, Cyclon.PROTOCOL_NAME);
                membershipProtocol = new Cyclon(PROTO_CHANNELS, overlayProps, myself);
                babel.registerProtocol(membershipProtocol);
                break;
            case "none":
                break;
            default:
                logger.error("Overlay {} is invalid", cli.membershipProtocol);
                System.exit(1);
        }

        logger.info("Loading dissemination {}", cli.service);
        Flood floodGossip = null;
        switch (cli.service) {
            case "flood":
                Translate.addId(Flood.PROTO_ID, Flood.PROTO_NAME);
                floodGossip = new Flood(PROTO_CHANNELS, overlayProps, myself);
                babel.registerProtocol(floodGossip);
                break;
            default:
                logger.error("Dissemination {} is invalid", cli.service);
                System.exit(1);
        }

        List<String> appArgs = new ArrayList<>();
        if(cli.peerId != -1) {
            appArgs.add("peerID="+cli.peerId);
        } else {
            logger.error("Peer id not set?");
            throw new Exception("Peer id not set?");
        }
        appArgs.add("startT="+cli.startT);
        String[] appArgsArr = new String[appArgs.size()];
        appArgs.toArray(appArgsArr);
        Properties appProps = babel.loadConfig("src/main/java/protocols/app/config/"+cli.appConfig, appArgsArr);
        logger.info("Loading app with config {}", cli.appConfig);
        Application app = new Application(myself, appProps);
        babel.registerProtocol(app);


        GenericProtocol resEstMembershipProtocol = null;
        GenericProtocol resEstProtocol = null;
        Properties resEstOverlayProps = null;
        Foutakos foutakos = null;
        Properties foutakosProps = null;
        if (!cli.noOptimizationFlag) {
            // Launching resest overlay
            List<String> resEstOverlayArgs = new ArrayList<>();
            resEstOverlayArgs.add("hbInterval=1000");
            resEstOverlayArgs.add("hbTolerance=3000");
            resEstOverlayArgs.add("connectTimeout=2000");
            resEstOverlayArgs.add("hostcomp=port");
            if(!cli.bootstraps.equals("")) {
                resEstOverlayArgs.add("contacts="+cli.bootstraps);
            }
            if(cli.listenIP.equals("")) {
                if(cli.listenInterface.equals("")) {
                    logger.error("No IP or interface?");
                    throw new Exception("No IP nor interface...");
                } else {
                    resEstOverlayArgs.add("listenInterface="+cli.listenInterface);
                }
            } else {
                resEstOverlayArgs.add("address="+cli.listenIP);
            }
            if(cli.randomPort) {
                resEstOverlayArgs.add("port=" + (10000 + new Random().nextInt(1000)));
            } else {
                resEstOverlayArgs.add("port="+cli.listenPort);
            }
            resEstOverlayArgs.add("hpvRemovalMode="+cli.hpvRemovalMode);
            if(cli.peerId != -1) {
                resEstOverlayArgs.add("peerID="+cli.peerId);
            } else {
                logger.error("Peer id not set?");
                throw new Exception("Peer id not set?");
            }
            resEstOverlayArgs.add("startT="+cli.startT);
            String[] resEstOverlayArgsArr = new String[resEstOverlayArgs.size()];
            resEstOverlayArgs.toArray(resEstOverlayArgsArr);
            resEstOverlayProps = babel.loadConfig("src/main/java/protocols/optimization/resest/config/" + cli.resEstConfig, resEstOverlayArgsArr);
            addInterfaceIp(resEstOverlayProps);
            switch (resEstOverlayProps.getProperty("protocolBelow")) {
                case "hyparview":
                    Translate.addId(HyparViewResEst.PROTOCOL_ID, HyparViewResEst.PROTOCOL_NAME);
                    resEstMembershipProtocol = new HyparViewResEst(PROTO_CHANNELS, resEstOverlayProps, myself);
                    babel.registerProtocol(resEstMembershipProtocol);
                    break;
                case "hyparviewIntegrated":
                    Translate.addId(HyparViewIntegrated.PROTOCOL_ID, HyparViewIntegrated.PROTOCOL_NAME);
                    resEstMembershipProtocol = new HyparViewIntegrated(PROTO_CHANNELS, resEstOverlayProps, myself);
                    babel.registerProtocol(resEstMembershipProtocol);
                    break;
                case "cyclon":
                    Translate.addId(CyclonResEst.PROTOCOL_ID, CyclonResEst.PROTOCOL_NAME);
                    resEstMembershipProtocol = new CyclonResEst(PROTO_CHANNELS, resEstOverlayProps, myself);
                    babel.registerProtocol(resEstMembershipProtocol);
                    break;
                default:
                    logger.error("ResEst overlay {} is invalid", resEstOverlayProps.getProperty("protocolBelow"));
                    System.exit(1);
            }

            //Launching ResEst
            logger.info("Loading resest with config {}", cli.resEstConfig);
            Translate.addId(ResEst.PROTOCOL_ID, ResEst.PROTOCOL_NAME);
            resEstProtocol = new ResEst(PROTO_CHANNELS, resEstOverlayProps, myself);
            babel.registerProtocol(resEstProtocol);

            // Launching foutakos
            String[] foutakosArgsArr = new String[0];
            foutakosProps = babel.loadConfig("src/main/java/protocols/optimization/foutakos/config/"+cli.foutakosConfig, foutakosArgsArr);
            logger.info("Loading foutakos with config {}", cli.foutakosConfig);
            foutakos = new Foutakos(foutakosProps);
            babel.registerProtocol(foutakos);
        }


        app.init(appProps);
        floodGossip.init(overlayProps);
        if(cli.peerId != -1) {
            overlayProps.setProperty("peerID", Integer.toString(cli.peerId));
        } else {
            logger.error("Peer id not set?");
            throw new Exception("Peer id not set?");
        }
        if(resEstOverlayProps != null) {
            overlayProps.setProperty("distribution", resEstOverlayProps.getProperty("distribution"));
        }
        if(membershipProtocol != null) {
            membershipProtocol.init(overlayProps);
        }
        if (resEstMembershipProtocol != null) {
            resEstMembershipProtocol.init(resEstOverlayProps);
            resEstProtocol.init(resEstOverlayProps);
            foutakos.init(foutakosProps);
        }
        babel.start();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                logger.info("Goodbye");
            }
        });

        logger.info("Start: " + System.currentTimeMillis());
    }

    private static void addInterfaceIp(Properties props) throws SocketException, InvalidParameterException {
        String interfaceName;
        if ((interfaceName = props.getProperty("listenInterface")) != null) {
            String ip = InterfaceToIp.getIpOfInterface(interfaceName);
            if(ip != null)
                props.setProperty("address", ip);
            else {
                throw new InvalidParameterException("Property interface is set to " + interfaceName + ", but has no ip");
            }
        }
    }
}