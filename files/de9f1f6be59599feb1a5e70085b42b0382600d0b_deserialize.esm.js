import {HAS_NODE_BUFFER, DATA_TYPE, TYPE_HEADER} from "./constants.esm.js";
import {UTF8Decode} from "./helper.esm.js";
import {
	Int8, Int16, Int32, Int64, Int128,
	UInt8, UInt16, UInt32, UInt64, UInt128,
	Binary, ObjectId, Int256, Int512, UInt256, UInt512
} from "./types.esm.js";
import {IntVar, UIntVar} from "./types/uint-var.esm.js";


/**
 * @typedef {{use_native_types:Boolean=true}} DeserializeOptions
**/

/**
 * Function for sequential deserialization
 *
 * @param {ArrayBuffer|Buffer} buffer
 * @param {Number} [anchor=0]
 * @param {DeserializeOptions} options
 * @return {{value:*, buffer:ArrayBuffer}|undefined}
 * @private
**/
export function _deserialize(buffer, anchor=0, options={use_native_types:true}) {
	if ( HAS_NODE_BUFFER ) {
		if ( buffer instanceof Buffer ) {
			let buff = Buffer.alloc(buffer.length);
			buffer.copy(buff, 0);
			buffer = buff.buffer;
		}
	}
	
	let value;
	({ anchor, value } = __deserializeContent(buffer, anchor, options));
	return (value === undefined) ? undefined : {value, anchor};
}

/**
 * Deserialize ArrayBuffer
 * - result = headerBuffer + contentBuffer
 * - contentBuffer = typeBuffer + dataBuffer
 * @param {ArrayBuffer|Buffer} buffer
 * @param {DeserializeOptions} options
 * @returns {*}
 */
export function deserialize(buffer, options={use_native_types:true}) {
	const result = _deserialize(buffer, 0, options);
	if ( result === undefined ) {
		throw new TypeError('Wrong data format');
	}
	
	return result.value;
}

/**
 * Deserialize content
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value:*}}
 * @private
 */
function __deserializeContent(buffer, start, options) {
	let type, data;
	({ anchor: start, value: type } = __deserializeType(buffer, start, options));
	({ anchor: start, value: data } = __deserializeData(type, buffer, start, options));
	return { anchor: start, value: data };
}

/**
 * Deserialize type
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: string}} anchor: byteOffset
 * @private
 */
function __deserializeType(buffer, start, options) {
	let length = 2;
	let end = start + length;
	let type = null;

	
	if ( (buffer.byteLength - start) >= length ) {
		let typeData = new Uint8Array(buffer, start, length);
		Object.entries(TYPE_HEADER).forEach(([headerKey, headerVal]) => {
			let headerData = new Uint8Array(headerVal);
			if ((typeData[0] === headerData[0]) && (typeData[1] === headerData[1])) {
				type = headerKey.toLowerCase();
			}
		});
	}
	return { anchor: end, value: type };
}

/**
 * Deserialize data
 * @param {string} type
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor:number, value:*}}
 * @private
 */
function __deserializeData(type, buffer, start, options) {
	let result = {anchor:start, value:undefined};
	if (type === DATA_TYPE.NULL) {
		result = __deserializeNull(start, options);
	}
	else if (type === DATA_TYPE.FALSE || type === DATA_TYPE.TRUE) {
		result = __deserializeBoolean(type, start, options);
	}
	else if (type === DATA_TYPE.INT8) {
		result = __deserializeInt8(buffer, start, options);
	}
	else if (type === DATA_TYPE.INT16) {
		result = __deserializeInt16(buffer, start, options);
	}
	else if (type === DATA_TYPE.INT32) {
		result = __deserializeInt32(buffer, start, options);
	}
	else if (type === DATA_TYPE.INT64) {
		result = __deserializeInt64(buffer, start, options);
	}
	else if (type === DATA_TYPE.INT128) {
		result = __deserializeInt128(buffer, start, options);
	}
	else if (type === DATA_TYPE.INT256) {
		result = __deserializeInt256(buffer, start, options);
	}
	else if (type === DATA_TYPE.INT512) {
		result = __deserializeInt512(buffer, start, options);
	}
	else if (type === DATA_TYPE.INTVAR) {
		result = __deserializeIntVar(buffer, start, options);
	}
	else if (type === DATA_TYPE.UINT8) {
		result = __deserializeUInt8(buffer, start, options);
	}
	else if (type === DATA_TYPE.UINT16) {
		result = __deserializeUInt16(buffer, start, options);
	}
	else if (type === DATA_TYPE.UINT32) {
		result = __deserializeUInt32(buffer, start, options);
	}
	else if (type === DATA_TYPE.UINT64) {
		result = __deserializeUInt64(buffer, start, options);
	}
	else if (type === DATA_TYPE.UINT128) {
		result = __deserializeUInt128(buffer, start, options);
	}
	else if (type === DATA_TYPE.UINT256) {
		result = __deserializeUInt256(buffer, start, options);
	}
	else if (type === DATA_TYPE.UINT512) {
		result = __deserializeUInt512(buffer, start, options);
	}
	else if (type === DATA_TYPE.UINTVAR) {
		result = __deserializeUIntVar(buffer, start, options);
	}
	else if (type === DATA_TYPE.FLOAT32) {
		result = __deserializeFloat32(buffer, start, options);
	}
	else if (type === DATA_TYPE.FLOAT64) {
		result = __deserializeFloat64(buffer, start, options);
	}
	else if (type === DATA_TYPE.STRING) {
		result = __deserializeString(buffer, start, options);
	}
	else if (type === DATA_TYPE.ARRAY) {
		result = __deserializeArray(buffer, start, options);
	}
	else if (type === DATA_TYPE.ARRAY_START) {
		result = __deserializeArrayStreaming(buffer, start, options);
	}
	else if (type === DATA_TYPE.OBJECT) {
		result = __deserializeObject(buffer, start, options);
	}
	else if (type === DATA_TYPE.OBJECT_START) {
		result = __deserializeObjectStreaming(buffer, start, options);
	}
	else if (type === DATA_TYPE.DATE) {
		result = __deserializeDate(buffer, start, options);
	}
	else if (type === DATA_TYPE.OBJECTID) {
		result = __deserializeObjectId(buffer, start, options);
	}
	else if (type === DATA_TYPE.BINARY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = Binary.FromArrayBuffer(result.value);
	}
	else if (type === DATA_TYPE.ARRAY_BUFFER) {
		result = __deserializeArrayBuffer(buffer, start, options);
	}
	else if (type === DATA_TYPE.DATA_VIEW) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new DataView(result.value);
	}
	else if (type === DATA_TYPE.UINT8_ARRAY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new Uint8Array(result.value);
	}
	else if (type === DATA_TYPE.INT8_ARRAY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new Int8Array(result.value);
	}
	else if (type === DATA_TYPE.UINT16_ARRAY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new Uint16Array(result.value);
	}
	else if (type === DATA_TYPE.INT16_ARRAY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new Int16Array(result.value);
	}
	else if (type === DATA_TYPE.UINT32_ARRAY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new Uint32Array(result.value);
	}
	else if (type === DATA_TYPE.INT32_ARRAY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new Int32Array(result.value);
	}
	else if (type === DATA_TYPE.FLOAT32_ARRAY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new Float32Array(result.value);
	}
	else if (type === DATA_TYPE.FLOAT64_ARRAY) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = new Float64Array(result.value);
	}
	else if (type === DATA_TYPE.SPECIAL_BUFFER) {
		result = __deserializeArrayBuffer(buffer, start, options);
		result.value = HAS_NODE_BUFFER ? Buffer.from(result.value) : new Uint8Array(result.value);
	}
	
	return result;
}

/**
 * Deserialize null data
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: null}} anchor: byteOffset
 * @private
 */
function __deserializeNull(start, options) {
	return { anchor: start, value: null };
}

/**
 * Deserialize boolean data
 * @param {string} type
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: boolean}} anchor: byteOffset
 * @private
 */
function __deserializeBoolean(type, start, options) {
	let end = start;
	let data = type === DATA_TYPE.TRUE;
	return { anchor: end, value: data };
}

/**
 * Deserialize Int8 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Number|Int8}} anchor: byteOffset
 * @private
 */
function __deserializeInt8(buffer, start, options) {
	let end = start + 1;
	let dataView = new DataView(buffer);
	let data = dataView.getInt8(start);
	return { anchor: end, value:options.use_native_types ? data : Int8.from(data) };
}

/**
 * Deserialize Int16 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Number|Int16}} anchor: byteOffset
 * @private
 */
function __deserializeInt16(buffer, start, options) {
	let end = start + 2;
	let dataView = new DataView(buffer);
	let data = dataView.getInt16(start, true);
	return { anchor: end, value:options.use_native_types ? data : Int16.from(data) };
}

/**
 * Deserialize Int32 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value:number|Int32}} anchor: byteOffset
 * @private
 */
function __deserializeInt32(buffer, start, options) {
	let end = start + 4;
	let dataView = new DataView(buffer);
	let data = dataView.getInt32(start, true);
	return { anchor: end, value:options.use_native_types ? data : Int32.from(data) };
}

/**
 * Deserialize Int64 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Int64}} anchor: byteOffset
 * @private
 */
function __deserializeInt64(buffer, start, options) {
	let step = 4;
	let length = 2;
	let end = start + (step * length);
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint32(i, true));
	}
	let data = new Int64(new Uint32Array(dataArray));
	return { anchor: end, value: data };
}

/**
 * Deserialize Int128 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Int128}} anchor: byteOffset
 * @private
 */
function __deserializeInt128(buffer, start, options) {
	let step = 4;
	let length = 4;
	let end = start + (step * length);
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint32(i, true));
	}
	let data = new Int128(new Uint32Array(dataArray));
	return { anchor: end, value: data };
}

/**
 * Deserialize Int256 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Int256}} anchor: byteOffset
 * @private
 */
function __deserializeInt256(buffer, start, options) {
	let step = 4;
	let length = 8;
	let end = start + (step * length);
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint32(i, true));
	}
	let data = new Int256(new Uint32Array(dataArray));
	return { anchor: end, value: data };
}

/**
 * Deserialize Int512 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Int512}} anchor: byteOffset
 * @private
 */
function __deserializeInt512(buffer, start, options) {
	let step = 4;
	let length = 16;
	let end = start + (step * length);
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint32(i, true));
	}
	let data = new Int512(new Uint32Array(dataArray));
	return { anchor: end, value: data };
}

/**
 * Deserialize IntVar data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: IntVar}} anchor: byteOffset
 * @private
 */
function __deserializeIntVar(buffer, start, options) {
	const dataBuff = new Uint8Array(buffer);
	if ( dataBuff[start] > 127 ) {
		throw new Error( "Cannot support IntVar whose size is greater than 127 bytes" );
	}
	
	
	
	let index = 0, data_size = dataBuff[start], end = start + 1;
	const result_buffer = new Uint8Array(data_size);
	while( data_size-- > 0 ) {
		result_buffer[index] = dataBuff[end];
		index++; end++;
	}


	let data = new IntVar(result_buffer);
	return { anchor: end, value: data };
}

/**
 * Deserialize UInt8 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Number|UInt8}} anchor: byteOffset
 * @private
 */
function __deserializeUInt8(buffer, start, options) {
	let end = start + 1;
	let dataView = new DataView(buffer);
	let data = dataView.getUint8(start);
	return { anchor: end, value:options.use_native_types ? data : UInt8.from(data) };
}

/**
 * Deserialize UInt16 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Number|UInt16}} anchor: byteOffset
 * @private
 */
function __deserializeUInt16(buffer, start, options) {
	let end = start + 2;
	let dataView = new DataView(buffer);
	let data = dataView.getUint16(start, true);
	return { anchor: end, value:options.use_native_types ? data : UInt16.from(data) };
}

/**
 * Deserialize UInt32 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value:number|UInt32}} anchor: byteOffset
 * @private
 */
function __deserializeUInt32(buffer, start, options) {
	let end = start + 4;
	let dataView = new DataView(buffer);
	let data = dataView.getUint32(start, true);
	return { anchor: end, value:options.use_native_types ? data : UInt32.from(data) };
}

/**
 * Deserialize UInt64 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: UInt64}} anchor: byteOffset
 * @private
 */
function __deserializeUInt64(buffer, start, options) {
	let step = 4;
	let length = 2;
	let end = start + (step * length);
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint32(i, true));
	}
	let data = new UInt64(new Uint32Array(dataArray));
	return { anchor: end, value: data };
}

/**
 * Deserialize UInt128 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: UInt128}} anchor: byteOffset
 * @private
 */
function __deserializeUInt128(buffer, start, options) {
	let step = 4;
	let length = 4;
	let end = start + (step * length);
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint32(i, true));
	}
	let data = new UInt128(new Uint32Array(dataArray));
	return { anchor: end, value: data };
}

/**
 * Deserialize UInt256 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: UInt256}} anchor: byteOffset
 * @private
 */
function __deserializeUInt256(buffer, start, options) {
	let step = 4;
	let length = 8;
	let end = start + (step * length);
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint32(i, true));
	}
	let data = new UInt256(new Uint32Array(dataArray));
	return { anchor: end, value: data };
}

/**
 * Deserialize UInt512 data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: UInt512}} anchor: byteOffset
 * @private
 */
function __deserializeUInt512(buffer, start, options) {
	let step = 4;
	let length = 16;
	let end = start + (step * length);
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint32(i, true));
	}
	let data = new UInt512(new Uint32Array(dataArray));
	return { anchor: end, value: data };
}

/**
 * Deserialize UIntVar data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: UIntVar}} anchor: byteOffset
 * @private
 */
function __deserializeUIntVar(buffer, start, options) {
	const dataBuff = new Uint8Array(buffer);
	if ( dataBuff[start] > 127 ) {
		throw new Error( "Cannot support UIntVar whose size is greater than 127 bytes" );
	}
	
	
	
	let index = 0, data_size = dataBuff[start], end = start + 1;
	const result_buffer = new Uint8Array(data_size);
	while( data_size-- > 0 ) {
		result_buffer[index] = dataBuff[end];
		index++; end++;
	}


	let data = new UIntVar(result_buffer);
	return { anchor: end, value: data };
}

/**
 * Deserialize float data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: number}} anchor: byteOffset, value: double
 * @private
 */
function __deserializeFloat32(buffer, start, options) {
	let end = start + 4;
	let dataView = new DataView(buffer);
	let data = dataView.getFloat32(start, true);
	return { anchor: end, value: data };
}

/**
 * Deserialize double data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: number}} anchor: byteOffset, value: double
 * @private
 */
function __deserializeFloat64(buffer, start, options) {
	let end = start + 8;
	let dataView = new DataView(buffer);
	let data = dataView.getFloat64(start, true);
	return { anchor: end, value: data };
}

/**
 * Deserialize string data (use UTF8 decode)
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: string}} anchor: byteOffset, value: 32-bits length string
 * @private
 */
function __deserializeString(buffer, start, options) {
	let step = 1;
	let dataView = new DataView(buffer);
	let length = dataView.getUint32(start, true);
	start += 4;
	let end = start + length;
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint8(i));
	}
	let data = UTF8Decode(Uint8Array.from(dataArray).buffer);
	return { anchor: end, value: data };
}

/**
 * Deserialize short string data (use UTF8 decode)
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: string}} anchor: byteOffset, value: 16-bits length string
 * @private
 */
function __deserializeShortString(buffer, start, options) {
	let step = 1;
	let dataView = new DataView(buffer);
	let length = dataView.getUint16(start, true);
	start += 2;
	let end = start + length;
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint8(i));
	}
	let data = UTF8Decode(Uint8Array.from(dataArray).buffer);
	return { anchor: end, value: data };
}

/**
 * Deserialize array data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: *[]}} anchor: byteOffset
 * @private
 */
function __deserializeArray(buffer, start, options) {
	let dataView = new DataView(buffer);
	let length = dataView.getUint32(start, true);
	start += 4;
	let end = start + length;
	let data = [];
	while (start < end) {
		let subType, subData;
		({ anchor: start, value: subType } = __deserializeType(buffer, start, options));
		({ anchor: start, value: subData } = __deserializeData(subType, buffer, start, options));
		data.push(subData);
	}
	return { anchor: end, value: data };
}

/**
 * Deserialize array data (use streaming)
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: *[]}} anchor: byteOffset
 * @private
 */
function __deserializeArrayStreaming(buffer, start, options) {
	let end = start;
	let dataView = new DataView(buffer);
	let endData = new Uint16Array(TYPE_HEADER.ARRAY_END);
	let data = [];
	while (start < buffer.byteLength) {
		let tmpType = dataView.getUint16(start, true);
		if (tmpType === endData[0]) {
			end += 2;
			break;
		}
		
		let subType, subData;
		({ anchor: start, value: subType } = __deserializeType(buffer, start, options));
		({ anchor: start, value: subData } = __deserializeData(subType, buffer, start, options));
		data.push(subData);
		end = start;
	}
	return { anchor: end, value: data };
}

/**
 * Deserialize object data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: {}}} anchor: byteOffset
 * @private
 */
function __deserializeObject(buffer, start, options) {
	let dataView = new DataView(buffer);
	let length = dataView.getUint32(start, true);
	start += 4;
	let end = start + length;
	let data = {};
	while (start < end) {
		let subType, subKey, subData;
		({ anchor: start, value: subType } = __deserializeType(buffer, start, options));
		({ anchor: start, value: subKey } = __deserializeShortString(buffer, start, options));
		({ anchor: start, value: subData } = __deserializeData(subType, buffer, start, options));
		data[subKey] = subData;
	}
	return { anchor: end, value: data };
}

/**
 * Deserialize object data (use streaming)
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: {}}} anchor: byteOffset
 * @private
 */
function __deserializeObjectStreaming(buffer, start, options) {
	let end = start;
	let dataView = new DataView(buffer);
	let endData = new Uint16Array(TYPE_HEADER.OBJECT_END);
	let data = {};
	while (start < buffer.byteLength) {
		let tmpType = dataView.getUint16(start, true);
		if (tmpType === endData[0]) {
			end += 2;
			break;
		}

		let subType, subKey, subData;
		({ anchor: start, value: subType } = __deserializeType(buffer, start, options));
		({ anchor: start, value: subKey } = __deserializeShortString(buffer, start, options));
		({ anchor: start, value: subData } = __deserializeData(subType, buffer, start, options));
		data[subKey] = subData;
		end = start;
	}
	return { anchor: end, value: data };
}

/**
 * Deserialize date data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: Date}} anchor: byteOffset
 * @private
 */
function __deserializeDate(buffer, start, options) {
	let end = start + 8;
	let dataView = new DataView(buffer);
	let data = new Date(dataView.getFloat64(start, true));
	return { anchor: end, value: data };
}

/**
 * Deserialize ObjectId data
 * @param {ArrayBuffer} buffer
 * @param {number} start - byteOffset
 * @param {DeserializeOptions} options
 * @returns {{anchor: number, value: ObjectId}} anchor: byteOffset
 * @private
 */
function __deserializeObjectId(buffer, start, options) {
	let step = 1;
	let length = 12;
	let end = start + length;
	let dataView = new DataView(buffer);
	let dataArray = [];
	for (let i = start; i < end; i += step) {
		dataArray.push(dataView.getUint8(i));
	}
	let data = new ObjectId(Uint8Array.from(dataArray).buffer);
	return { anchor: end, value: data };
}

/**
 * Deserialize ArrayBuffer object
 * @param {ArrayBuffer} buffer
 * @param {Number} start
 * @returns {{anchor:Number, value:ArrayBuffer}}
 * @param {DeserializeOptions} options
 * @private
 */
function __deserializeArrayBuffer(buffer, start, options) {
	let end = start + 4;
	let [length] = new Uint32Array(buffer.slice(start, end));
	
	end = end + length;
	return {anchor:end, value:buffer.slice(start+4, end)};
}
