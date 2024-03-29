package me.DawnBudgie.PandoraHierarchy;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class Updater
{
  private Plugin plugin;
  private UpdateType type;
  private String versionTitle;
  private String versionLink;
  private long totalSize;
  private int sizeLine;
  private int multiplier;
  private boolean announce;
  private URL url;
  private File file;
  private Thread thread;
  private String[] noUpdateTag = { "-DEV", "-PRE", "-SNAPSHOT" };
  private String updateFolder = YamlConfiguration.loadConfiguration(new File("bukkit.yml")).getString("settings.update-folder");
  private UpdateResult result = UpdateResult.SUCCESS;
  public static enum UpdateResult
  {
    SUCCESS,  NO_UPDATE,  FAIL_DOWNLOAD,  FAIL_DBO,  FAIL_NOVERSION,  FAIL_BADSLUG,  UPDATE_AVAILABLE;
  }
  
  public static enum UpdateType
  {
    DEFAULT,  NO_VERSION_CHECK,  NO_DOWNLOAD;
  }
  
  public Updater(Plugin plugin, String slug, File file, UpdateType type, boolean announce)
  {
    this.plugin = plugin;
    this.type = type;
    this.announce = announce;
    this.file = file;
    try
    {
      this.url = new URL("http://dev.bukkit.org/server-mods/" + slug + "/files.rss");
    }
    catch (MalformedURLException ex)
    {
      plugin.getLogger().warning("The author of this plugin (" + (String)plugin.getDescription().getAuthors().get(0) + ") has misconfigured their Auto Update system");
      plugin.getLogger().warning("The project slug given ('" + slug + "') is invalid. Please nag the author about this.");
      this.result = UpdateResult.FAIL_BADSLUG;
    }
    this.thread = new Thread(new UpdateRunnable());
    this.thread.start();
  }
  
  public UpdateResult getResult()
  {
    waitForThread();
    return this.result;
  }
  
  public long getFileSize()
  {
    waitForThread();
    return this.totalSize;
  }
  
  public String getLatestVersionString()
  {
    waitForThread();
    return this.versionTitle;
  }
  
  public void waitForThread()
  {
    if (this.thread.isAlive()) {
      try
      {
        this.thread.join();
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }
    }
  }
  
  private void saveFile(File folder, String file, String u)
  {
    if (!folder.exists()) {
      folder.mkdir();
    }
    BufferedInputStream in = null;
    FileOutputStream fout = null;
    try
    {
      URL url = new URL(u);
      int fileLength = url.openConnection().getContentLength();
      in = new BufferedInputStream(url.openStream());
      fout = new FileOutputStream(folder.getAbsolutePath() + "/" + file);
      
      byte[] data = new byte[1024];
      if (this.announce) {
        this.plugin.getLogger().info("About to download a new update: " + this.versionTitle);
      }
      long downloaded = 0L;
      while ((in.read(data, 0, 1024)) != -1)
      {
        int count1 = 0;
        downloaded += count1;
        fout.write(data, 0, count1);
        int percent = (int)(downloaded * 100L / fileLength);
        if ((this.announce & percent % 10 == 0)) {
          this.plugin.getLogger().info("Downloading update: " + percent + "% of " + fileLength + " bytes.");
        }
      }
      for (File xFile : new File("plugins/" + this.updateFolder).listFiles()) {
        if (xFile.getName().endsWith(".zip")) {
          xFile.delete();
        }
      }
      File dFile = new File(folder.getAbsolutePath() + "/" + file);
      if (dFile.getName().endsWith(".zip")) {
        unzip(dFile.getCanonicalPath());
      }
      if (this.announce) {
        this.plugin.getLogger().info("Finished updating.");
      }
    }
    catch (Exception ex)
    {
      this.plugin.getLogger().warning("The auto-updater tried to download a new update, but was unsuccessful.");
      this.result = UpdateResult.FAIL_DOWNLOAD;
      try
      {
        if (in != null) {
          in.close();
        }
        if (fout != null) {
          fout.close();
        }
      }
      catch (Exception localException1) {}
    }
    finally
    {
      try
      {
        if (in != null) {
          in.close();
        }
        if (fout != null) {
          fout.close();
        }
      }
      catch (Exception localException2) {}
    }
  }
  
  private void unzip(String canonicalPath) {
	// TODO Auto-generated method stub
	
}

public boolean pluginFile(String name)
  {
    for (File file : new File("plugins").listFiles()) {
      if (file.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }
  
  @SuppressWarnings("null")
private String getFile(String link)
  {
    String download = null;
    try
    {
      URL url = new URL(link);
      URLConnection urlConn = url.openConnection();
      InputStreamReader inStream = new InputStreamReader(urlConn.getInputStream());
      BufferedReader buff = new BufferedReader(inStream);
      
      int counter = 0;
      while ((buff.readLine()) != null)
      {
        String line1 = null;
        counter++;
        if (line1.contains("<li class=\"user-action user-action-download\">"))
        {
          download = line1.split("<a href=\"")[1].split("\">Download</a>")[0];
        }
        else if (line1.contains("<dt>Size</dt>"))
        {
          this.sizeLine = (counter + 1);
        }
        else if (counter == this.sizeLine)
        {
          String size = line1.replaceAll("<dd>", "").replaceAll("</dd>", "");
          this.multiplier = (size.contains("MiB") ? 1048576 : 1024);
          size = size.replace(" KiB", "").replace(" MiB", "");
          this.totalSize = (long) ((Double.parseDouble(size) * this.multiplier));
        }
      }
      urlConn = null;
      inStream = null;
      buff.close();
      buff = null;
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      this.plugin.getLogger().warning("The auto-updater tried to contact dev.bukkit.org, but was unsuccessful.");
      this.result = UpdateResult.FAIL_DBO;
      return null;
    }
    return download;
  }
  
  private boolean versionCheck(String title)
  {
    if (this.type != UpdateType.NO_VERSION_CHECK)
    {
      String version = this.plugin.getDescription().getVersion();
      if (title.split(" v").length == 2)
      {
        String remoteVersion = title.split(" v")[1].split(" ")[0];
        int remVer = -1;int curVer = 0;
        try
        {
          remVer = calVer(remoteVersion).intValue();
          curVer = calVer(version).intValue();
        }
        catch (NumberFormatException nfe)
        {
          remVer = -1;
        }
        if ((hasTag(version)) || (version.equalsIgnoreCase(remoteVersion)) || (curVer >= remVer))
        {
          this.result = UpdateResult.NO_UPDATE;
          return false;
        }
      }
      else
      {
        this.plugin.getLogger().warning("The author of this plugin (" + (String)this.plugin.getDescription().getAuthors().get(0) + ") has misconfigured their Auto Update system");
        this.plugin.getLogger().warning("Files uploaded to BukkitDev should contain the version number, seperated from the name by a 'v', such as PluginName v1.0");
        this.plugin.getLogger().warning("Please notify the author of this error.");
        this.result = UpdateResult.FAIL_NOVERSION;
        return false;
      }
    }
    return true;
  }
  
  private Integer calVer(String s)
    throws NumberFormatException
  {
    if (s.contains("."))
    {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++)
      {
        Character c = Character.valueOf(s.charAt(i));
        if (Character.isLetterOrDigit(c.charValue())) {
          sb.append(c);
        }
      }
      return Integer.valueOf(Integer.parseInt(sb.toString()));
    }
    return Integer.valueOf(Integer.parseInt(s));
  }
  
  private boolean hasTag(String version)
  {
    for (String string : this.noUpdateTag) {
      if (version.contains(string)) {
        return true;
      }
    }
    return false;
  }
  
  private boolean readFeed()
  {
    try
    {
      String title = "";
      String link = "";
      
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      
      InputStream in = read();
      if (in != null)
      {
        XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
        while (eventReader.hasNext())
        {
          XMLEvent event = eventReader.nextEvent();
          if (event.isStartElement())
          {
            if (event.asStartElement().getName().getLocalPart().equals("title"))
            {
              event = eventReader.nextEvent();
              title = event.asCharacters().getData();
            }
            else if (event.asStartElement().getName().getLocalPart().equals("link"))
            {
              event = eventReader.nextEvent();
              link = event.asCharacters().getData();
            }
          }
          else if (event.isEndElement()) {
            if (event.asEndElement().getName().getLocalPart().equals("item"))
            {
              this.versionTitle = title;
              this.versionLink = link;
              
              break;
            }
          }
        }
        return true;
      }
      return false;
    }
    catch (XMLStreamException e)
    {
      this.plugin.getLogger().warning("Could not reach dev.bukkit.org for update checking. Is it offline?");
    }
    return false;
  }
  
  private InputStream read()
  {
    try
    {
      return this.url.openStream();
    }
    catch (IOException e)
    {
      this.plugin.getLogger().warning("Could not reach BukkitDev file stream for update checking. Is dev.bukkit.org offline?");
    }
    return null;
  }
  
  private class UpdateRunnable
    implements Runnable
  {
    private UpdateRunnable() {}
    
    public void run()
    {
      if (Updater.this.url != null) {
        if (Updater.this.readFeed()) {
          if (Updater.this.versionCheck(Updater.this.versionTitle))
          {
            String fileLink = Updater.this.getFile(Updater.this.versionLink);
            if ((fileLink != null) && (Updater.this.type != Updater.UpdateType.NO_DOWNLOAD))
            {
              String name = Updater.this.file.getName();
              if (fileLink.endsWith(".zip"))
              {
                String[] split = fileLink.split("/");
                name = split[(split.length - 1)];
              }
              Updater.this.saveFile(new File("plugins/" + Updater.this.updateFolder), name, fileLink);
            }
            else
            {
              Updater.this.result = Updater.UpdateResult.UPDATE_AVAILABLE;
            }
          }
        }
      }
    }
  }
}
