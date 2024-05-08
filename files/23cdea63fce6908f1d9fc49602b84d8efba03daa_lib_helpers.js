var exports = {};

function isEqualDate(dateA, dateB) {
  return (
    dateA.getUTCFullYear() === dateB.getUTCFullYear()
    && dateA.getUTCMonth() === dateB.getUTCMonth()
    && dateA.getUTCDate() === dateB.getUTCDate()
    && dateA.getTimezoneOffset() === dateB.getTimezoneOffset()
  );
}
exports.isEqualDate = isEqualDate;

function isEqualDateTime(dateA, dateB) {
  return dateA.getTime() === dateB.getTime();
}
exports.isEqualDateTime = isEqualDateTime;

function isAfter(dateA, dateB) {
  return dateA.getTime() > dateB.getTime();
}
exports.isAfter = isAfter;

function isBefore(dateA, dateB) {
  return dateA.getTime() < dateB.getTime();
}
exports.isBefore = isBefore;

function isBetween(date, start, end) {
  return isAfter(date, start) && isBefore(date, end);
}
exports.isBetween = isBetween;

module.exports = exports;
