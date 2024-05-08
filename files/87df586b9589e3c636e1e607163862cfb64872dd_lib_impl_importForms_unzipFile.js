var async = require('async');
var mkdirp = require('mkdirp');
var unzip = require('yauzl');
var fs = require('fs');
var path = require('path');
var logger = require('../../common/logger').getLogger();


/**
 * makeExportDirectory - Making a directory in the same structure as the zip file.
 *
 * @param  {object} params
 * @param  {object} params.entry  Single Zip file entry
 * @param  {object} params.zipfile Reference To THe Parent Zip File.
 * @param  {string} params.workingDir Directory being unzipped into.
 * @param  {function} callback
 */
function makeExportDirectory(params, callback) {
  var newDirPath = params.workingDir + "/" + params.entry.fileName;
  mkdirp(newDirPath, function(err) {
    if (err) {
      logger.debug("Error making directory " + newDirPath, err);
      return callback(err);
    }
    params.zipfile.readEntry();
    return callback(err);
  });
}


/**
 * streamFileEntry - Streaming a single uncompressed file from the zip file to the working folder.
 *
 * The folder structure of the zip file is maintained.
 *
 * @param  {object} params
 * @param  {object} params.zipfile Reference to the parent zip file
 * @param  {object} params.entry   Single Zip file entry
 * @param  {string} params.workingDir Directory being unzipped into.
 * @param  {function} callback
 * @return {type}          description
 */
function streamFileEntry(params, callback) {
  params.zipfile.openReadStream(params.entry, function(err, readStream) {
    if (err) {
      return callback(err);
    }
    // ensure parent directory exists
    var newFilePath = params.workingDir + "/" + params.entry.fileName;
    mkdirp(path.dirname(newFilePath), function(err) {
      if (err) {
        logger.debug("Error making directory " + newFilePath, err);
        return callback(err);
      }
      readStream.pipe(fs.createWriteStream(newFilePath));
      readStream.on("end", function() {
        params.zipfile.readEntry();
        callback();
      });
      readStream.on('error', function(err) {
        callback(err);
      });
    });
  });
}


/**
 * unzipWorker - Async Queue worker to pipe the unzipped file to a folder.
 *
 * @param  {object} unzipTask description
 * @param  {function} workerCb  description
 */
function unzipWorker(unzipTask, workerCb) {
  var zipfile = unzipTask.zipfile;
  var entry = unzipTask.entry;
  var workingDir = unzipTask.workingDir;
  if (/\/$/.test(entry.fileName)) {
    // directory file names end with '/'
    makeExportDirectory({
      entry: entry,
      zipfile: zipfile,
      workingDir: workingDir
    }, workerCb);
  } else {
    // file entry
    streamFileEntry({
      zipfile: zipfile,
      entry: entry,
      workingDir: workingDir
    }, workerCb);
  }
}


/**
 * unzipToWorkingDir - Unzipping a file to a working directory.
 *
 * Using a queue to control the number of files decompressing at a time.
 *
 * @param  {object} params
 * @param  {string} params.zipFilePath Path to the ZIP file to unzip.
 * @param  {number} params.queueConcurrency Concurrency Of the async queue
 * @param  {string} params.workingDir Unzip Working Directory
 * @param  {function} callback
 */
function unzipToWorkingDir(params, callback) {
  var unzipError;

  var queue = async.queue(unzipWorker, params.queueConcurrency || 5);

  //Pushing a single file unzip to the queue.
  function getQueueEntry(zipfile) {
    return function queueEntry(entry) {
      queue.push({
        zipfile: zipfile,
        workingDir: params.workingDir,
        entry: entry
      }, function(err) {
        if (err) {
          logger.debug("Error unzipping file params.zipFilePath", err);
          //If one of the files has failed to unzip correctly. No point in continuing to unzip. Close the zip file.
          zipfile.close();
        }
      });
    };
  }

  unzip.open(params.zipFilePath, {lazyEntries: true}, function(err, zipfile) {
    if (err) {
      return callback(err);
    }

    zipfile.on("entry", getQueueEntry(zipfile));
    zipfile.readEntry();

    zipfile.on('error', function(err) {
      logger.error("Error unzipping Zip File " + params.zipFilePath, err);
      unzipError = err;
    });

    zipfile.on('close', function() {
      //When the queue is empty and the zip file has finisihed scanning files, then the unzip is finished
      logger.debug("Zip File " + params.zipFilePath + " Unzipped");
      callback(unzipError);
    });
  });
}


module.exports = unzipToWorkingDir;
