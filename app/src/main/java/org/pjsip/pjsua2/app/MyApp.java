/* $Id: MyApp.java 5102 2015-05-28 07:14:24Z riza $ */
/*
 * Copyright (C) 2013 Teluu Inc. (http://www.teluu.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.pjsip.pjsua2.app;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.BuddyConfig;
import org.pjsip.pjsua2.ContainerNode;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.JsonDocument;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;
import org.pjsip.pjsua2.StringVector;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import java.io.File;
import java.util.ArrayList;


class MyLogWriter extends LogWriter {
    @Override
    public void write(LogEntry entry) {
        System.out.println(entry.getMsg());
    }
}

class MyAccountConfig {
    public AccountConfig accCfg = new AccountConfig();
    public ArrayList<BuddyConfig> buddyCfgs = new ArrayList<BuddyConfig>();

    public void readObject(ContainerNode node) {
        try {
            ContainerNode acc_node = node.readContainer("Account");
            accCfg.readObject(acc_node);
            ContainerNode buddies_node = acc_node.readArray("buddies");
            buddyCfgs.clear();
            while (buddies_node.hasUnread()) {
                BuddyConfig bud_cfg = new BuddyConfig();
                bud_cfg.readObject(buddies_node);
                buddyCfgs.add(bud_cfg);
            }
        } catch (Exception e) {}
    }

    public void writeObject(ContainerNode node) {
        try {
            ContainerNode acc_node = node.writeNewContainer("Account");
            accCfg.writeObject(acc_node);
            ContainerNode buddies_node = acc_node.writeNewArray("buddies");
            for (int j = 0; j < buddyCfgs.size(); j++) {
                buddyCfgs.get(j).writeObject(buddies_node);
            }
        } catch (Exception e) {}
    }
}

public class MyApp {
    public static Endpoint ep = new Endpoint();
    public static MyAppObserver observer;

    static {
        try {
            System.loadLibrary("openh264");
            System.loadLibrary("yuv");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("UnsatisfiedLinkError: " + e.getMessage());
            System.out.println("This could be safely ignored if you " +
                    "don't need video.");
        }
        System.loadLibrary("pjsua2");
        System.out.println("Library loaded");
    }

    private final String configName = "pjsua2.json";
    private final int SIP_PORT = 5060;
    private final int LOG_LEVEL = 4;
    public ArrayList<MyAccount> accList = new ArrayList<MyAccount>();
    private ArrayList<MyAccountConfig> accCfgs =
            new ArrayList<MyAccountConfig>();
    private EpConfig epConfig = new EpConfig();
    private TransportConfig sipTpConfig = new TransportConfig();
    private String appDir;
    /* Maintain reference to log writer to avoid premature cleanup by GC */
    private MyLogWriter logWriter;

    public void init(MyAppObserver obs, String app_dir) {
        init(obs, app_dir, false);
    }

    public void init(MyAppObserver obs, String app_dir,
                     boolean own_worker_thread) {
        observer = obs;
        appDir = app_dir;

	/* Create endpoint */
        try {
            ep.libCreate();
        } catch (Exception e) {
            return;
        }


	/* Load config */
        String configPath = appDir + "/" + configName;
        File f = new File(configPath);
        if (f.exists()) {
            loadConfig(configPath);
        } else {
	    /* Set 'default' values */
            sipTpConfig.setPort(SIP_PORT);
        }

	/* Override log level setting */
        epConfig.getLogConfig().setLevel(LOG_LEVEL);
        epConfig.getLogConfig().setConsoleLevel(LOG_LEVEL);

	/* Set log config. */
        LogConfig log_cfg = epConfig.getLogConfig();
        logWriter = new MyLogWriter();
        log_cfg.setWriter(logWriter);
        log_cfg.setDecor(log_cfg.getDecor() &
                ~ (pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue()));

	/* Set ua config. */
        UaConfig ua_cfg = epConfig.getUaConfig();
        ua_cfg.setUserAgent("Pjsua2 Android Messiah " + ep.libVersion().getFull());
        StringVector stun_servers = new StringVector();
        stun_servers.add("stun.pjsip.org");
        ua_cfg.setStunServer(stun_servers);
        if (own_worker_thread) {
            ua_cfg.setThreadCnt(0);
            ua_cfg.setMainThreadOnly(true);
        }

	/* Init endpoint */
        try {
            ep.libInit(epConfig);
        } catch (Exception e) {
            return;
        }

	/* Create transports. */
        try {
            ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                    sipTpConfig);
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP,
                    sipTpConfig);
        } catch (Exception e) {
            System.out.println(e);
        }

	/* Create accounts. */
        for (int i = 0; i < accCfgs.size(); i++) {
            MyAccountConfig my_cfg = accCfgs.get(i);

	    /* Customize account config */
            my_cfg.accCfg.getNatConfig().setIceEnabled(true);
            my_cfg.accCfg.getVideoConfig().setAutoTransmitOutgoing(true);
            my_cfg.accCfg.getVideoConfig().setAutoShowIncoming(true);

            MyAccount acc = addAcc(my_cfg.accCfg);
            if (acc == null) { continue; }

	    /* Add Buddies */
            for (int j = 0; j < my_cfg.buddyCfgs.size(); j++) {
                BuddyConfig bud_cfg = my_cfg.buddyCfgs.get(j);
                acc.addBuddy(bud_cfg);
            }
        }

	/* Start. */
        try {
            ep.libStart();
        } catch (Exception e) {
            return;
        }
    }

    private void loadConfig(String filename) {
        JsonDocument json = new JsonDocument();

        try {
	    /* Load file */
            json.loadFile(filename);
            ContainerNode root = json.getRootContainer();

	    /* Read endpoint config */
            epConfig.readObject(root);

	    /* Read transport config */
            ContainerNode tp_node = root.readContainer("SipTransport");
            sipTpConfig.readObject(tp_node);

	    /* Read account configs */
            accCfgs.clear();
            ContainerNode accs_node = root.readArray("accounts");
            while (accs_node.hasUnread()) {
                MyAccountConfig acc_cfg = new MyAccountConfig();
                acc_cfg.readObject(accs_node);
                accCfgs.add(acc_cfg);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

	/* Force delete json now, as I found that Java somehow destroys it
	* after lib has been destroyed and from non-registered thread.
	*/
        json.delete();
    }

    public MyAccount addAcc(AccountConfig cfg) {
        MyAccount acc = new MyAccount(cfg);
        try {
            acc.create(cfg);
        } catch (Exception e) {
            acc = null;
            return null;
        }

        accList.add(acc);
        return acc;
    }

    public void delAcc(MyAccount acc) {
        accList.remove(acc);
    }

    public void deinit() {
        String configPath = appDir + "/" + configName;
        saveConfig(configPath);

	/* Try force GC to avoid late destroy of PJ objects as they should be
	* deleted before lib is destroyed.
	*/
        Runtime.getRuntime().gc();

	/* Shutdown pjsua. Note that Endpoint destructor will also invoke
	* libDestroy(), so this will be a test of double libDestroy().
	*/
        try {
            ep.libDestroy();
        } catch (Exception ignored) {}

	/* Force delete Endpoint here, to avoid deletion from a non-
	* registered thread (by GC?).
	*/
        ep.delete();
        ep = null;
    }

    private void saveConfig(String filename) {
        JsonDocument json = new JsonDocument();

        try {
	    /* Write endpoint config */
            json.writeObject(epConfig);

	    /* Write transport config */
            ContainerNode tp_node = json.writeNewContainer("SipTransport");
            sipTpConfig.writeObject(tp_node);

	    /* Write account configs */
            buildAccConfigs();
            ContainerNode accs_node = json.writeNewArray("accounts");
            for (int i = 0; i < accCfgs.size(); i++) {
                accCfgs.get(i).writeObject(accs_node);
            }

	    /* Save file */
            json.saveFile(filename);
        } catch (Exception e) {}

	/* Force delete json now, as I found that Java somehow destroys it
	* after lib has been destroyed and from non-registered thread.
	*/
        json.delete();
    }

    private void buildAccConfigs() {
	/* Sync accCfgs from accList */
        accCfgs.clear();
        for (int i = 0; i < accList.size(); i++) {
            MyAccount acc = accList.get(i);
            MyAccountConfig my_acc_cfg = new MyAccountConfig();
            my_acc_cfg.accCfg = acc.cfg;

            my_acc_cfg.buddyCfgs.clear();
            for (int j = 0; j < acc.buddyList.size(); j++) {
                MyBuddy bud = acc.buddyList.get(j);
                my_acc_cfg.buddyCfgs.add(bud.cfg);
            }

            accCfgs.add(my_acc_cfg);
        }
    }
}


