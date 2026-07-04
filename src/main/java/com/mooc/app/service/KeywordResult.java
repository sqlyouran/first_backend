package com.mooc.app.service;

import java.util.UUID;

public record KeywordResult(UUID id, String name, String slug, String type, double score) {}
