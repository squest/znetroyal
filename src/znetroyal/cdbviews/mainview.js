

function (doc) {
	if (doc.tContentGroup && doc.cgLevel) {
		emit(doc.cgLevel, doc);
	}
}