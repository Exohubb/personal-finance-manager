package com.syfe.personalfinancemanager.exception

class BadRequestException(message: String) : RuntimeException(message)

class ResourceNotFoundException(message: String) : RuntimeException(message)

class AppAccessDeniedException(message: String) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)
