package app.vireo.data

class StaleSourceException :
    IllegalStateException("Content source changed during request")
