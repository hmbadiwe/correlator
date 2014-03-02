package com.example.service

import org.apache.lucene.store.{RAMDirectory, MMapDirectory, Directory}
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.util.Version
import java.io.{IOException, File}

/**
 * Created with IntelliJ IDEA.
 * User: hmbadiwe
 * Date: 3/1/14
 * Time: 8:49 PM
 * To change this template use File | Settings | File Templates.
 */
trait SetupDirectory {
  def setupDirectory( directory: Directory ) : Directory = {
    val indexWriter = createIndexWriter( directory )
    indexWriter.close()
    directory
  }
  def createIndexWriter( directory : Directory ) : IndexWriter = {
    val analyzer = new KeywordAnalyzer
    val indexWriter = {

      val iwConfig: IndexWriterConfig = new IndexWriterConfig(
        Version.LUCENE_43,
        analyzer
      )
      iwConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
      val iw = new IndexWriter(
        directory,
        iwConfig
      )
      iw
    }
    indexWriter
  }
  def createDirectory( filePath: String, isSetup : Boolean ) : Directory = {
    val fsDirectory = new File(filePath)
    if (!fsDirectory.isDirectory) {
      if (!fsDirectory.mkdir()) {
        throw new IOException(s"Error creating directory: ${filePath}")
      }
    }
    val directory = new MMapDirectory(fsDirectory)
    if( !isSetup )  setupDirectory( directory )
    directory
  }

  def directoryList( count : Int, basePath : Option[ String ] = None ) : List[ Directory ] = {
    (0 until count).map { index =>
      basePath.map{ path => createDirectory( s"${path}/${index}", false ) }.getOrElse( new RAMDirectory())
    }.toList
  }
}
