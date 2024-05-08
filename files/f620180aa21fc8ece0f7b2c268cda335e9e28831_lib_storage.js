//Import dependencies
var fs = require('./fs.js');
var json = require('./json.js');
var task = require('./task.js');
var string = require('./string.js');

//Json storage
var storage = function(opt, cb)
{
  //Check the options object
  if(typeof opt !== 'object'){ throw new Error('No options object provided'); }

  //Check the storage path option
  if(typeof opt.path !== 'string'){ throw new Error('No storage path provided'); }

  //Save the database path
  this.path = path.resolve(process.cwd(), opt.path);

  //Save the default encoding
  this.encoding = (typeof opt.encoding === 'string') ? opt.encoding : 'utf8';

  //Create the database directory
  fs.mkdir(this.path, function(error)
  {
    //Check the callback method
    if(typeof cb !== 'function'){ return; }

    //Call the callback method
    return cb(error);
  });

  //Return this
  return this;
};

//Get a document file path
storage.prototype.file = function(id)
{
  //Return the document path
  return path.join(this.path, id + '.json');
};

//Get the list of documents available
storage.prototype.documents = function(cb)
{
  //Read the directory
  return fs.readdir(this.path, function(error, files)
  {
    //Check the error
    if(error){ return cb(error, null);}

    //Output files list
    var list = {};

    //For each file path
    files.forEach(function(file)
    {
      //Check the file extension
      if(path.extname(file) !== '.json'){ return; }

      //Get the file id
      var id = path.basename(file, '.json');

      //Add the json file path
      list[id] = { id: id, path: file };
    });

    //Call the callback with the list of documents
    return cb(null, list);
  });
};

//Get a document
storage.prototype.get = function(id, cb)
{
  //Get the file path
  var file = this.file(id);

  //Register the task queue
  return task.add(file, function(next)
  {
    //Read the content of the document
    return json.read(file, function(error, data)
    {
      //Next task on the queue
      next();

      //Return the content of the document
      return cb(error, data);
    });
  });
};

//Set a document content
storage.prototype.set = function(id, obj, cb)
{
  //Parse the callback
  cb = (typeof obj === 'function') ? obj : cb;

  //Get the document id
  var file_id = (typeof id !== 'string') ? string.unique() : id.trim();

  //Get the file content
  var file_content = (typeof id === 'object') ? id : obj;

  //Get the document path
  var file_path = this.file(file_id);

  //Register the task in the queue
  return task.add(file_path, function(next)
  {
    //Read the file path
    return json.read(file_path, function(error, data)
    {
      //Check the error
      if(error)
      {
        //Check for not found error
        if(error.code === 'ENOENT')
        {
          //File does not exists, initialize the data object
          data = {};
        }
        else
        {
          //Next task
          next();

          //Another error -> call the callback with the error
          return cb(error, null);
        }
      }

      //Assign the new values
      data = Object.assign(data, file_content);

      //Add the document id
      data._id = file_id;

      //Save to the file
      return json.write(file_path, data, function(error)
      {
        //Next task
        next();

        //Call the callback
        return cb(error, data);
      });
    });
  });
};

//Remove a document
storage.prototype.remove = function(id, cb)
{
  //Get the file path
  var file = this.file(id);

  //Register the task queue
  return task.add(file, function(next)
  {
    //Delete the file
    return fs.unlink(file, function(error)
    {
      //Next task in the queue
      next();

      //Call the callback method
      return cb(error)
    });
  });
};

//Exports the storage object 
module.exports = storage;
