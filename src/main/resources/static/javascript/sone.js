/* Sone JavaScript functions. */

/* jQuery overrides. */
oldGetJson = jQuery.prototype.getJSON;
jQuery.prototype.getJSON = function(url, data, successCallback, errorCallback) {
	if (typeof errorCallback == "undefined") {
		return oldGetJson(url, data, successCallback);
	}
	if (jQuery.isFunction(data)) {
		errorCallback = successCallback;
		successCallback = data;
		data = null;
	}
	return jQuery.ajax({
		data: data,
		error: errorCallback,
		success: successCallback,
		url: url
	});
}

function isOnline() {
	return $("#sone").hasClass("online");
}

function registerInputTextareaSwap(inputSelector, defaultText, inputFieldName, optional, dontUseTextarea) {
	$(inputSelector).each(function() {
		textarea = $(dontUseTextarea ? "<input type=\"text\" name=\"" + inputFieldName + "\">" : "<textarea name=\"" + inputFieldName + "\"></textarea>").blur(function() {
			if ($(this).val() == "") {
				$(this).hide();
				inputField = $(this).data("inputField");
				inputField.show().removeAttr("disabled").addClass("default");
				inputField.val(defaultText);
			}
		}).hide().data("inputField", $(this)).val($(this).val());
		$(this).after(textarea);
		(function(inputField, textarea) {
			inputField.focus(function() {
				$(this).hide().attr("disabled", "disabled");
				textarea.show().focus();
			});
			if (inputField.val() == "") {
				inputField.addClass("default");
				inputField.val(defaultText);
			} else {
				inputField.hide().attr("disabled", "disabled");
				textarea.show();
			}
			$(inputField.get(0).form).submit(function() {
				if (!optional && (textarea.val() == "")) {
					return false;
				}
			});
		})($(this), textarea);
	});
}

/* hide all the “create reply” forms until a link is clicked. */
function addCommentLinks() {
	if (!isOnline()) {
		return;
	}
	$("#sone .post").each(function() {
		postId = $(this).attr("id");
		addCommentLink(postId, $(this));
	});
}

/**
 * Adds a “comment” link to all status lines contained in the given element.
 *
 * @param postId
 *            The ID of the post
 * @param element
 *            The element to add a “comment” link to
 */
function addCommentLink(postId, element) {
	commentElement = (function(postId) {
		var commentElement = $("<div><span>Comment</span></div>").addClass("show-reply-form").click(function() {
			replyElement = $("#sone .post#" + postId + " .create-reply");
			replyElement.removeClass("hidden");
			replyElement.removeClass("light");
			(function(replyElement) {
				replyElement.find("input.reply-input").blur(function() {
					if ($(this).hasClass("default")) {
						replyElement.addClass("light");
					}
				}).focus(function() {
					replyElement.removeClass("light");
				});
			})(replyElement);
			replyElement.find("input.reply-input").focus();
		});
		return commentElement;
	})(postId);
	element.find(".create-reply").addClass("hidden");
	element.find(".status-line .time").each(function() {
		$(this).after(commentElement.clone(true));
	});
}

var translations = {};

/**
 * Retrieves the translation for the given key and calls the callback function.
 * The callback function takes a single parameter, the translated string.
 *
 * @param key
 *            The key of the translation string
 * @param callback
 *            The callback function
 */
function getTranslation(key, callback) {
	if (key in translations) {
		callback(translations[key]);
		return;
	}
	$.getJSON("ajax/getTranslation.ajax", {"key": key}, function(data, textStatus) {
		if ((data != null) && data.success) {
			translations[key] = data.value;
			callback(data.value);
		}
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

/**
 * Filters the given Sone ID, replacing all “~” characters by an underscore.
 *
 * @param soneId
 *            The Sone ID to filter
 * @returns The filtered Sone ID
 */
function filterSoneId(soneId) {
	return soneId.replace(/[^a-zA-Z0-9-]/g, "_");
}

/**
 * Updates the status of the given Sone.
 *
 * @param soneId
 *            The ID of the Sone to update
 * @param status
 *            The status of the Sone (“idle”, “unknown”, “inserting”,
 *            “downloading”)
 * @param modified
 *            Whether the Sone is modified
 * @param locked
 *            Whether the Sone is locked
 * @param lastUpdated
 *            The date and time of the last update (formatted for display)
 */
function updateSoneStatus(soneId, name, status, modified, locked, lastUpdated) {
	$("#sone .sone." + filterSoneId(soneId)).
		toggleClass("unknown", status == "unknown").
		toggleClass("idle", status == "idle").
		toggleClass("inserting", status == "inserting").
		toggleClass("downloading", status == "downloading").
		toggleClass("modified", modified);
	$("#sone .sone." + filterSoneId(soneId) + " .lock").toggleClass("hidden", locked);
	$("#sone .sone." + filterSoneId(soneId) + " .unlock").toggleClass("hidden", !locked);
	$("#sone .sone." + filterSoneId(soneId) + " .last-update span.time").text(lastUpdated);
	$("#sone .sone." + filterSoneId(soneId) + " .profile-link a").text(name);
}

/**
 * Enhances a “delete” button so that the confirmation is done on the same page.
 *
 * @param buttonId
 *            The selector of the button
 * @param text
 *            The text to show on the button
 * @param deleteCallback
 *            The callback that actually deletes something
 */
function enhanceDeleteButton(buttonId, text, deleteCallback) {
	button = $(buttonId);
	(function(button) {
		newButton = $("<button></button>").addClass("confirm").hide().text(text).click(function() {
			$(this).fadeOut("slow");
			deleteCallback();
			return false;
		}).insertAfter(button);
		(function(button, newButton) {
			button.click(function() {
				button.fadeOut("slow", function() {
					newButton.fadeIn("slow");
					$(document).one("click", function() {
						if (this != newButton.get(0)) {
							newButton.fadeOut(function() {
								button.fadeIn();
							});
						}
					});
				});
				return false;
			});
		})(button, newButton);
	})(button);
}

/**
 * Enhances a post’s “delete” button.
 *
 * @param buttonId
 *            The selector of the button
 * @param postId
 *            The ID of the post to delete
 * @param text
 *            The text to replace the button with
 */
function enhanceDeletePostButton(buttonId, postId, text) {
	enhanceDeleteButton(buttonId, text, function() {
		$.getJSON("ajax/deletePost.ajax", { "post": postId, "formPassword": $("#sone #formPassword").text() }, function(data, textStatus) {
			if (data == null) {
				return;
			}
			if (data.success) {
				$("#sone .post#" + postId).slideUp();
			} else if (data.error == "invalid-post-id") {
				alert("Invalid post ID given!");
			} else if (data.error == "auth-required") {
				alert("You need to be logged in.");
			} else if (data.error == "not-authorized") {
				alert("You are not allowed to delete this post.");
			}
		}, function(xmlHttpRequest, textStatus, error) {
			/* ignore error. */
		});
	});
}

/**
 * Enhances a reply’s “delete” button.
 *
 * @param buttonId
 *            The selector of the button
 * @param replyId
 *            The ID of the reply to delete
 * @param text
 *            The text to replace the button with
 */
function enhanceDeleteReplyButton(buttonId, replyId, text) {
	enhanceDeleteButton(buttonId, text, function() {
		$.getJSON("ajax/deleteReply.ajax", { "reply": replyId, "formPassword": $("#sone #formPassword").text() }, function(data, textStatus) {
			if (data == null) {
				return;
			}
			if (data.success) {
				$("#sone .reply#" + replyId).slideUp();
			} else if (data.error == "invalid-reply-id") {
				alert("Invalid reply ID given!");
			} else if (data.error == "auth-required") {
				alert("You need to be logged in.");
			} else if (data.error == "not-authorized") {
				alert("You are not allowed to delete this reply.");
			}
		}, function(xmlHttpRequest, textStatus, error) {
			/* ignore error. */
		});
	});
}

function getFormPassword() {
	return $("#sone #formPassword").text();
}

function getSoneElement(element) {
	return $(element).parents(".sone");
}

/**
 * Generates a list of Sones by concatening the names of the given sones with a
 * new line character (“\n”).
 *
 * @param sones
 *            The sones to format
 * @returns {String} The created string
 */
function generateSoneList(sones) {
	var soneList = "";
	$.each(sones, function() {
		if (soneList != "") {
			soneList += "\n";
		}
		soneList += this.name;
	});
	return soneList;
}

/**
 * Returns the ID of the Sone that this element belongs to.
 *
 * @param element
 *            The element to locate the matching Sone ID for
 * @returns The ID of the Sone, or undefined
 */
function getSoneId(element) {
	return getSoneElement(element).find(".id").text();
}

function getPostElement(element) {
	return $(element).parents(".post");
}

function getPostId(element) {
	return getPostElement(element).attr("id");
}

function getReplyElement(element) {
	return $(element).parents(".reply");
}

function getReplyId(element) {
	return getReplyElement(element).attr("id");
}

function likePost(postId) {
	$.getJSON("ajax/like.ajax", { "type": "post", "post" : postId, "formPassword": getFormPassword() }, function(data, textStatus) {
		if ((data == null) || !data.success) {
			return;
		}
		$("#sone .post#" + postId + " > .inner-part > .status-line .like").addClass("hidden");
		$("#sone .post#" + postId + " > .inner-part > .status-line .unlike").removeClass("hidden");
		updatePostLikes(postId);
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

function unlikePost(postId) {
	$.getJSON("ajax/unlike.ajax", { "type": "post", "post" : postId, "formPassword": getFormPassword() }, function(data, textStatus) {
		if ((data == null) || !data.success) {
			return;
		}
		$("#sone .post#" + postId + " > .inner-part > .status-line .unlike").addClass("hidden");
		$("#sone .post#" + postId + " > .inner-part > .status-line .like").removeClass("hidden");
		updatePostLikes(postId);
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

function updatePostLikes(postId) {
	$.getJSON("ajax/getLikes.ajax", { "type": "post", "post": postId }, function(data, textStatus) {
		if ((data != null) && data.success) {
			$("#sone .post#" + postId + " > .inner-part > .status-line .likes").toggleClass("hidden", data.likes == 0)
			$("#sone .post#" + postId + " > .inner-part > .status-line .likes span.like-count").text(data.likes);
			$("#sone .post#" + postId + " > .inner-part > .status-line .likes > span").attr("title", generateSoneList(data.sones));
		}
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

function likeReply(replyId) {
	$.getJSON("ajax/like.ajax", { "type": "reply", "reply" : replyId, "formPassword": getFormPassword() }, function(data, textStatus) {
		if ((data == null) || !data.success) {
			return;
		}
		$("#sone .reply#" + replyId + " .status-line .like").addClass("hidden");
		$("#sone .reply#" + replyId + " .status-line .unlike").removeClass("hidden");
		updateReplyLikes(replyId);
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

function unlikeReply(replyId) {
	$.getJSON("ajax/unlike.ajax", { "type": "reply", "reply" : replyId, "formPassword": getFormPassword() }, function(data, textStatus) {
		if ((data == null) || !data.success) {
			return;
		}
		$("#sone .reply#" + replyId + " .status-line .unlike").addClass("hidden");
		$("#sone .reply#" + replyId + " .status-line .like").removeClass("hidden");
		updateReplyLikes(replyId);
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

function updateReplyLikes(replyId) {
	$.getJSON("ajax/getLikes.ajax", { "type": "reply", "reply": replyId }, function(data, textStatus) {
		if ((data != null) && data.success) {
			$("#sone .reply#" + replyId + " .status-line .likes").toggleClass("hidden", data.likes == 0)
			$("#sone .reply#" + replyId + " .status-line .likes span.like-count").text(data.likes);
			$("#sone .reply#" + replyId + " .status-line .likes > span").attr("title", generateSoneList(data.sones));
		}
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

/**
 * Posts a reply and calls the given callback when the request finishes.
 *
 * @param postId
 *            The ID of the post the reply refers to
 * @param text
 *            The text to post
 * @param callbackFunction
 *            The callback function to call when the request finishes (takes 3
 *            parameters: success, error, replyId)
 */
function postReply(postId, text, callbackFunction) {
	$.getJSON("ajax/createReply.ajax", { "formPassword" : getFormPassword(), "post" : postId, "text": text }, function(data, textStatus) {
		if (data == null) {
			/* TODO - show error */
			return;
		}
		if (data.success) {
			callbackFunction(true, null, data.reply);
		} else {
			callbackFunction(false, data.error);
		}
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

/**
 * Requests information about the reply with the given ID.
 *
 * @param replyId
 *            The ID of the reply
 * @param callbackFunction
 *            A callback function (parameters soneId, soneName, replyTime,
 *            replyDisplayTime, text, html)
 */
function getReply(replyId, callbackFunction) {
	$.getJSON("ajax/getReply.ajax", { "reply" : replyId }, function(data, textStatus) {
		if ((data != null) && data.success) {
			callbackFunction(data.soneId, data.soneName, data.time, data.displayTime, data.text, data.html);
		}
	}, function(xmlHttpRequest, textStatus, error) {
		/* ignore error. */
	});
}

/**
 * Ajaxifies the given notification by replacing the form with AJAX.
 *
 * @param notification
 *            jQuery object representing the notification.
 */
function ajaxifyNotification(notification) {
	notification.find("form.dismiss").submit(function() {
		return false;
	});
	notification.find("form.dismiss button").click(function() {
		$.getJSON("ajax/dismissNotification.ajax", { "formPassword" : getFormPassword(), "notification" : notification.attr("id") }, function(data, textStatus) {
			/* dismiss in case of error, too. */
			notification.slideUp();
		}, function(xmlHttpRequest, textStatus, error) {
			/* ignore error. */
		});
	});
	return notification;
}

function getStatus() {
	$.getJSON("ajax/getStatus.ajax", {}, function(data, textStatus) {
		if ((data != null) && data.success) {
			/* process Sone information. */
			$.each(data.sones, function(index, value) {
				updateSoneStatus(value.id, value.name, value.status, value.modified, value.locked, value.lastUpdated);
			});
			/* process notifications. */
			$.each(data.notifications, function(index, value) {
				oldNotification = $("#sone #notification-area .notification#" + value.id);
				notification = ajaxifyNotification(createNotification(value.id, value.text, value.dismissable)).hide();
				if (oldNotification.length != 0) {
					oldNotification.replaceWith(notification.show());
				} else {
					$("#sone #notification-area").append(notification);
					notification.slideDown();
				}
			});
			$.each(data.removedNotifications, function(index, value) {
				$("#sone #notification-area .notification#" + value.id).slideUp();
			});
			/* do it again in 5 seconds. */
			setTimeout(getStatus, 5000);
		} else {
			/* data.success was false, wait 30 seconds. */
			setTimeout(getStatus, 30000);
		}
	}, function(xmlHttpRequest, textStatus, error) {
		/* something really bad happend, wait a minute. */
		setTimeout(getStatus, 60000);
	})
}

/**
 * Creates a new notification.
 *
 * @param id
 *            The ID of the notificaiton
 * @param text
 *            The text of the notification
 * @param dismissable
 *            <code>true</code> if the notification can be dismissed by the
 *            user
 */
function createNotification(id, text, dismissable) {
	notification = $("<div></div>").addClass("notification").attr("id", id);
	if (dismissable) {
		dismissForm = $("#sone #notification-area #notification-dismiss-template").clone().removeClass("hidden").removeAttr("id")
		dismissForm.find("input[name=notification]").val(id);
		notification.append(dismissForm);
	}
	notification.append(text);
	return notification;
}

//
// EVERYTHING BELOW HERE IS EXECUTED AFTER LOADING THE PAGE
//

$(document).ready(function() {

	/* this initializes the status update input field. */
	getTranslation("WebInterface.DefaultText.StatusUpdate", function(text) {
		registerInputTextareaSwap("#sone #update-status .status-input", text, "text", false, false);
	})

	/* this initializes all reply input fields. */
	getTranslation("WebInterface.DefaultText.Reply", function(text) {
		registerInputTextareaSwap("#sone input.reply-input", text, "text", false, false);
		addCommentLinks();
	})

	/* replaces all “post reply!” forms with AJAX. */
	$("#sone .create-reply button:submit").click(function() {
		$(this.form).submit(function() {
			return false;
		});
		inputField = $(this.form).find(":input:enabled").get(0);
		postId = getPostId($(inputField));
		text = $(inputField).val();
		$(inputField).val("");
		postReply(postId, text, function(success, error, replyId) {
			if (success) {
				getReply(replyId, function(soneId, soneName, replyTime, replyDisplayTime, text, html) {
					newReply = $(html).insertBefore("#sone .post#" + postId + " .create-reply");
					$("#sone .post#" + postId + " .create-reply").addClass("hidden");
					getTranslation("WebInterface.Confirmation.DeleteReplyButton", function(deleteReplyText) {
						enhanceDeleteReplyButton("#sone .post#" + postId + " .reply#" + replyId + " .delete button", replyId, deleteReplyText);
					});
					newReply.find(".status-line .like").submit(function() {
						likeReply(getReplyId(this));
						return false;
					});
					newReply.find(".status-line .unlike").submit(function() {
						unlikeReply(getReplyId(this));
						return false;
					});
					addCommentLink(postId, newReply);
				});
			} else {
				alert(error);
			}
		});
		return false;
	});

	/* replace all “delete” buttons with javascript. */
	getTranslation("WebInterface.Confirmation.DeletePostButton", function(text) {
		deletePostText = text;
		getTranslation("WebInterface.Confirmation.DeleteReplyButton", function(text) {
			deleteReplyText = text;
			$("#sone .post").each(function() {
				postId = $(this).attr("id");
				enhanceDeletePostButton("#sone .post#" + postId + " > .inner-part > .status-line .delete button", postId, deletePostText);
				(function(postId) {
					$("#sone .post#" + postId + " .reply").each(function() {
						replyId = $(this).attr("id");
						(function(postId, reply, replyId) {
							reply.find(".delete button").each(function() {
								enhanceDeleteReplyButton("#sone .post#" + postId + " .reply#" + replyId + " .delete button", replyId, deleteReplyText);
							})
						})(postId, $(this), replyId);
					});
				})(postId);
			});
		});
	});

	/* hides all replies but the latest two. */
	getTranslation("WebInterface.ClickToShow.Replies", function(text) {
		$("#sone .post .replies").each(function() {
			allReplies = $(this).find(".reply");
			if (allReplies.length > 2) {
				newHidden = false;
				for (replyIndex = 0; replyIndex < (allReplies.length - 2); ++replyIndex) {
					$(allReplies[replyIndex]).addClass("hidden");
					newHidden |= $(allReplies[replyIndex]).hasClass("new");
				}
				clickToShowElement = $("<div></div>").addClass("click-to-show");
				if (newHidden) {
					clickToShowElement.addClass("new");
				}
				(function(clickToShowElement, allReplies, text) {
					clickToShowElement.text(text);
					clickToShowElement.click(function() {
						allReplies.removeClass("hidden");
						clickToShowElement.addClass("hidden");
					});
				})(clickToShowElement, allReplies, text);
				$(allReplies[0]).before(clickToShowElement);
			}
		});
	});

	/*
	 * convert all “follow”, “unfollow”, “lock”, and “unlock” links to something
	 * nicer.
	 */
	$("#sone .follow").submit(function() {
		var followElement = this;
		$.getJSON("ajax/followSone.ajax", { "sone": getSoneId(this), "formPassword": getFormPassword() }, function() {
			$(followElement).addClass("hidden");
			$(followElement).parent().find(".unfollow").removeClass("hidden");
		});
		return false;
	});
	$("#sone .unfollow").submit(function() {
		var unfollowElement = this;
		$.getJSON("ajax/unfollowSone.ajax", { "sone": getSoneId(this), "formPassword": getFormPassword() }, function() {
			$(unfollowElement).addClass("hidden");
			$(unfollowElement).parent().find(".follow").removeClass("hidden");
		});
		return false;
	});
	$("#sone .lock").submit(function() {
		var lockElement = this;
		$.getJSON("ajax/lockSone.ajax", { "sone" : getSoneId(this), "formPassword" : getFormPassword() }, function() {
			$(lockElement).addClass("hidden");
			$(lockElement).parent().find(".unlock").removeClass("hidden");
		});
		return false;
	});
	$("#sone .unlock").submit(function() {
		var unlockElement = this;
		$.getJSON("ajax/unlockSone.ajax", { "sone" : getSoneId(this), "formPassword" : getFormPassword() }, function() {
			$(unlockElement).addClass("hidden");
			$(unlockElement).parent().find(".lock").removeClass("hidden");
		});
		return false;
	});

	/* convert all “like” buttons to javascript functions. */
	$("#sone .post > .inner-part > .status-line .like").submit(function() {
		likePost(getPostId(this));
		return false;
	});
	$("#sone .post > .inner-part > .status-line .unlike").submit(function() {
		unlikePost(getPostId(this));
		return false;
	});
	$("#sone .post .reply .status-line .like").submit(function() {
		likeReply(getReplyId(this));
		return false;
	});
	$("#sone .post .reply .status-line .unlike").submit(function() {
		unlikeReply(getReplyId(this));
		return false;
	});

	/* process all existing notifications, ajaxify dismiss buttons. */
	$("#sone #notification-area .notification").each(function() {
		ajaxifyNotification($(this));
	});

	/* activate status polling. */
	setTimeout(getStatus, 5000);
});
