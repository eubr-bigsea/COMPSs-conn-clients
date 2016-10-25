package es.bsc.conn.clients.jclouds;

import java.io.File;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Location;
import org.junit.Test;

import com.google.common.io.Files;

import es.bsc.conn.clients.jclouds.JCloudsClient;


public class JCloudsClientTest {

    private static final Logger LOGGER = LogManager.getLogger("Console");


    @Test
    public void testOpenStack() throws Exception {
        LOGGER.info("Begin test OpenStack");
        LOGGER.debug("# PROVIDERS:");
        for (String key : JCloudsClient.appProviders.keySet()) {
            LOGGER.debug(key + ",");
        }
        LOGGER.debug("# PROVIDERS:");
        for (String key : JCloudsClient.apiProviders.keySet()) {
            LOGGER.debug(key + ",");
        }
        LOGGER.debug("#");

        // OpenStack
        JCloudsClient jcc = new JCloudsClient("admin:jejarque", "jejarque.14", "openstack-nova", "http://bscgrid28.bsc.es:5000/v2.0/");
        // Stub
        // JCloudsClient jcc = new JCloudsClient("admin:jejarque", "jejarque.14", "stub",
        // "http://bscgrid28.bsc.es:5000/v2.0/");

        LOGGER.debug("# Locations in provider");
        for (Location loc : jcc.getLocations()) {
            LOGGER.debug("locations: " + loc.getId());
        }
        LOGGER.debug("# Images in provider");
        for (Image img : jcc.getImages()) {
            LOGGER.debug("image: " + img.getId() + " description: " + img.getDescription());
        }

        LOGGER.debug("# HW Profiles in provider");
        for (Hardware prof : jcc.getHardwareProfiles()) {
            LOGGER.debug("HW Prof: " + prof.getId() + " Name: " + prof.getName());
        }

        LOGGER.debug("# Nodes in provider");
        for (ComputeMetadata node : jcc.getNodes()) {
            LOGGER.debug("Node: " + node.getId() + " description: " + node.getName());
        }
        LOGGER.debug("# Creating new nodes");
        Template t = jcc.createTemplate("regionOne/2", "regionOne/576c3c9a-f301-471a-9de5-a06e0e0e0960", new TemplateOptions());
        Set<? extends NodeMetadata> newNodes = jcc.createVMS("dummyapp", 1, t);
        LOGGER.debug("# Destroying new nodes");
        for (ComputeMetadata node : newNodes) {
            LOGGER.debug("Node: " + node.getId() + " description: " + node.getName());
            jcc.destroyNode(node.getId());
        }
    }

    @Test
    public void testGoogleComputeCloud() throws Exception {
        LOGGER.info("Begin test GCE");
        LOGGER.debug("# PROVIDERS:");
        for (String key : JCloudsClient.appProviders.keySet()) {
            LOGGER.debug(key + ",");
        }
        LOGGER.debug("# PROVIDERS:");
        for (String key : JCloudsClient.apiProviders.keySet()) {
            LOGGER.debug(key + ",");
        }
        LOGGER.debug("#");

        // OpenStack
        JCloudsClient jcc = new JCloudsClient("562200327616-s3opol8kq6l7r4e14t5hnilbjddt3prf@developer.gserviceaccount.com",
                "/home/jorgee/Credentials/GCE/compss-test-sc-15-0ae9f38f31f6.json", "google-compute-engine", "");
        // Stub
        // JCloudsClient jcc = new JCloudsClient("admin:jejarque", "jejarque.14", "stub",
        // "http://bscgrid28.bsc.es:5000/v2.0/");

        /*
         * System.out.println("# Locations in provider"); for (Location loc:jcc.getLocations()){
         * System.out.println("locations: "+loc.getId()); }
         */
        LOGGER.debug("# Images in provider");
        for (Image img : jcc.getImages()) {
            LOGGER.debug("image: " + img.getId() + " description: " + img.getDescription());
        }

        /*
         * System.out.println("# HW Profiles in provider"); for (Hardware prof:jcc.getHardwareProfiles()){
         * System.out.println("HW Prof: "+prof.getId()+" Name: "+prof.getName()); }
         */
        LOGGER.debug("# Nodes in provider");
        for (ComputeMetadata node : jcc.getNodes()) {
            LOGGER.debug("Node: " + node.getId() + " description: " + node.getName());
        }

        LOGGER.debug("# Creating new nodes");
        TemplateOptions to = new TemplateOptions();
        // to = to.networks("default");
        int[] ports = new int[] { 22, 43000, 43001, 43002, 43003 };
        to = to.inboundPorts(ports);
        to = to.authorizePublicKey(Files.toString(new File(System.getProperty("user.home") + "/.ssh/id_rsa.pub"), UTF_8));
        // to = to.authorizePublicKey("ssh-rsa
        // AAAAB3NzaC1yc2EAAAADAQABAAABAQDAY28FT1jkU7vyB0NF5wB5xTtdBILrFrUbA5qj6DMAUvdDzgggJKS15Ul6
        // DBQW2uXMgLspUTgTPJinzCobScuc9y0cQBEQ0KKoqJerZHePTPPTl1MwmHd5Eylii4y6KgUXXH/zscGZHMXVjy/8
        // l/252JttnapJAeMps4VCVJiDXcStbEMia9y9z43vfsClMoff+8QPRAdM4khxqbtsFpMBPZCf/OEEubR8xbrlZ/dh
        // Ez+0fz2ExrOcOmHHlXu4Oz88xgUuJEez7W7wtiTirV1bN9keeazPCwNGp7okpcHon3bwQ3/pXoebJVyKLV/rs4B1pnafLdYux+5P+7o7vmEJ");
        to = to.overrideLoginPrivateKey(Files.toString(new File(System.getProperty("user.home") + "/.ssh/id_rsa"), UTF_8));
        /*
         * to = to.installPrivateKey("-----BEGIN RSA PRIVATE KEY-----"+
         * "MIIEpAIBAAKCAQEAwGNvBU9Y5FO78gdDRecAecU7XQSC6xa1GwOao+gzAFL3Q84I"+
         * "ICSkteVJegwUFtrlzIC7KVE4EzyYp8wqG0nLnPctHEARENCiqKiXq2R3j0zz05dT"+
         * "MJh3eRMpYouMuioFF1x/87HBmRzF1Y8v/Jf9udibbZ2qSQHjKbOFQlSYg13ErWxD"+
         * "Imvcvc+N737ApTKH3/vED0QHTOJIcam7bBaTAT2Qn/zhBLm0fMW65Wf3YRM/tH89"+
         * "hMaznDphx5V7uDs/PMYFLiRHs+1u8LYk4q1dWzfZHnmszwsDRqe6JKXB6J928EN/"+
         * "6V6HmyVcii1f67OAdaZ2ny3WLsfuT/u6O75hCQIDAQABAoIBAF3PHk9khV+wRLCb"+
         * "Qf5PyTeXKH8OuBeRlvV5KGpLyrKZwd/aErQ5qebXyqAsS49pZSv2iUx4QfN/VKBd"+
         * "ORrdPN9o1yIzSx773JSIwIveDT5es3W1D+deMMNkyIU4roIIAzuE6w8U2eWi1gRw"+
         * "MWHlat638/HbMzEuLmojExNo1IUY1zvnphiFNVbxQMHEXb6Qr2q6LzpYMECrECqi"+
         * "NQHpetjWPWsycbZUdkj2XKlZBj2yw6wrHIol5rLx63PBiR2ZQRix9brkgZlxmEq+"+
         * "sqXE/tTYGS8/RlJdR3iF3ckbybr7AD79vZ0ZCuDtKWccIVfr6em9ZTgn7PNDHYSB"+
         * "10nY/FECgYEA5pqR0rukvkWlIX1uns3ECG5wneWTodx26eQ94G6AfMtlLXRuf7Kx"+
         * "20fPiX6iZqDCOPsprtN8MPcuINRRG7q9PFGwR/h1xOIofMUKm1i+YIuSRQhBkbab"+
         * "wqNhv5YlJ+QOPewP/WmGVoVbbXAATwoYP72l1lB83mqU/6oEDsOQQ/sCgYEA1ZN0"+
         * "faqWr3/mTOPXrKo65Bk/1HI90WOC2dgpPRvIraHLAcbXwPc7f6Hf0C6d9ldVBVKU"+
         * "Ij7PY+heEzXAJ3iuwshTHbg2FeLSvjl1BOeZ598h9Q6vZLABlUBwUo7PaENmWp1b"+
         * "HIMwFrqnuW0gNm++bChCIfH6iDo/rfRSAO73G8sCgYEAielC8H6cBdbs7Nxw/bQd"+
         * "WtWBsEyOQCzzSOnhP4mWegvCFK8fLmuWBogpzBbZ1PqrbRx4Tz/XdAk/ow6W+zhw"+
         * "19Qa0s/6zqZahFPrBgDKyj5uWa6YWCaXfI3tdYC78+Flyw6UeX9UggX7KbXj2WE2"+
         * "I1iuBz/XjcS8GzJ4fsPiUWUCgYEAwrrhWbryDS4GY1DSUqIbc+H5qtBGwOWEZu+K"+
         * "Gid8/6MnH4WXLl+9JhAHqx7186eI44N5gQfXu/Yf4E//1X1ZiktwTQaqeaYIFFz6"+
         * "7u/kCeAObAtpq9o2d0j7oKqJPT1G05PpgMO0UuT5DD4NQtT7wE2sjpq8nPzPFuJO"+
         * "6n73W6UCgYBp5zJqgDlSzBbUS6IvQWndst1im3zUS9kxxlid2ww8ZsbuCNyT7d7v"+
         * "YoBZdttql2dhsx8lUQicQ2rCnz9iYooes9UfyBAlPPHL804NHk9S4A3Fc/r5f4pg"+
         * "G2QcySYFYOx4TS0buZ7QttUD9N6zIybc+cQaTLz896MNPOPx2zO4WA=="+ "-----END RSA PRIVATE KEY-----");
         */
        Template t = jcc.createTemplate(
                "https://www.googleapis.com/compute/v1/projects/compss-test-sc-15/zones/europe-west1-b/machineTypes/n1-standard-2",
                "https://www.googleapis.com/compute/v1/projects/compss-test-sc-15/global/images/ubuntu-java-7", to);
        // Template t = jcc.createTemplate("https://www.googleapis.com/compute/v1/projects/compss-test-sc-15/
        // zones/europe-west1-b/machineTypes/n1-standard-2","https://www.googleapis.com/compute/v1/
        // projects/debian-cloud/global/images/debian-7-wheezy-v20151104", to);
        jcc.createVMS("dummyapp", 1, t);
        Thread.sleep(120_000);

        LOGGER.debug("# Nodes in provider");
        for (ComputeMetadata node : jcc.getNodes()) {
            LOGGER.debug("Node: " + node.getId() + " description: " + node.getName());
        }
        /*
         * System.out.println("# Destroying new nodes"); for (ComputeMetadata node:newNodes){
         * System.out.println("Node: " + node.getId() + " description: " + node.getName());
         * jcc.destroyNode(node.getId()); }
         */
    }

}
